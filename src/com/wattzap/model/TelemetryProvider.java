/*
 * This file is part of Wattzap Community Edition.
 *
 * Wattzap Community Edtion is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wattzap Community Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wattzap.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wattzap.model;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.Telemetry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Checks all (added) dataHandlers and computes periodically telemetry on settings
 * basis (speed, cadence, heart rate, power, trainer load).
 * @author Jarek
 */
public enum TelemetryProvider implements MessageCallback
{
    INSTANCE;

    private final Logger logger = LogManager.getLogger("Telemetry");

    // existing sensor subsystems, currently only single ANT+..
    private final List<SubsystemIntf> subsystems = new ArrayList<>();

    // list of handlers for source data (read from subsystems, or computed on telemetry..)
    private final List<SourceDataProcessorIntf> handlers = new ArrayList<>();
    private final Map<SourceDataEnum, SensorIntf> sensors = new HashMap<>();

    /* current "location" and training time, filled in telemetry to be reported */
    private final Telemetry t = new Telemetry();
    private double distance = 0.0; // [km]
    private long runtime = 0; // [ms]

    private Thread runner = null;
    private int[] lastProvides = new int[SourceDataEnum.values().length];

    public TelemetryProvider initialize() {
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM_REMOVED, this);
        MessageBus.INSTANCE.register(Messages.HANDLER, this);
        MessageBus.INSTANCE.register(Messages.HANDLER_REMOVED, this);
        MessageBus.INSTANCE.register(Messages.STARTPOS, this);
        MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
        MessageBus.INSTANCE.register(Messages.CLOSE, this);
        MessageBus.INSTANCE.register(Messages.START, this);
        MessageBus.INSTANCE.register(Messages.STOP, this);
        return this;
    }

    private static final Map<Integer, String> pauseMsgKeys = new HashMap<>();
    static {
        pauseMsgKeys.put(-1, "not_started");
        pauseMsgKeys.put(1, "no_movement");
        pauseMsgKeys.put(2, "manual_pause");
        pauseMsgKeys.put(100, "end_of_training");
    }
    public static String pauseMsg(Telemetry t, boolean nullIfUnknown) {
        if (!pauseMsgKeys.containsKey(t.getPaused())) {
            if (nullIfUnknown) {
                return null;
            } else {
                return String.format("unknown[%d]", t.getPaused());
            }
        }
        String key = pauseMsgKeys.get(t.getPaused());
        if (UserPreferences.INSTANCE.messages.containsKey(key)) {
            return UserPreferences.INSTANCE.messages.getString(key);
        }
        return key;
    }
    public static String pauseMsg(Telemetry t) {
        return pauseMsg(t, true);
    }

    // subsystems to be handled
    public List<SubsystemIntf> getSubsystems() {
        return subsystems;
    }
    // all registered handlers: sensors, telemetryProcessors and others
    public List<SourceDataProcessorIntf> getHandlers() {
        return handlers;
    }

    // selected data handlers
    public SensorIntf getSensor(SourceDataEnum data) {
        return sensors.get(data);
    }
    public void setSensor(SourceDataEnum data, SourceDataProcessorIntf sensor) {
        if (sensor == null) {
            sensors.remove(data);
        } else {
            if (sensor instanceof SensorIntf) {
                sensors.put(data, (SensorIntf) sensor);
            } else {
                throw new UnsupportedOperationException(
                        "Non-sensor handler " + sensor.getPrettyName());
            }
        }
    }

    /* Main loop: get all data, process it and send current telemetry, then sleep some time.
     * Advance time and distance as well.
     * Next step.. set indicators
     */
    private void threadLoop() {
        logger.debug("TelemetryProvider started");
        for (int i = 0; i < lastProvides.length; i++) {
            lastProvides[i] = -1;
        }
        // Wait all handlers reinitialize to show what is wrong with configuration.
        // One cannot start at once, but nobody does :)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        while (!Thread.currentThread().isInterrupted()) {
            long start = System.currentTimeMillis();

            for (SourceDataEnum prop : SourceDataEnum.values()) {
                double value = prop.getDefault();
                int provides = 0;

                SensorIntf sensor = getSensor(prop);
                if ((sensor != null) && (sensor.provides(prop))) {
                    if (sensor.getModificationTime(prop) >= start - 5000) {
                        provides++;
                        value = sensor.getValue(prop);
                    }
                }

                for (SourceDataProcessorIntf handler :  handlers) {
                    // if handler is active and provides property
                    if ((handler.getLastMessageTime() == -1) && (handler.provides(prop))) {
                        provides++;
                        if (prop == SourceDataEnum.PAUSE) {
                            if (value < handler.getValue(prop)) {
                                value = handler.getValue(prop);
                            }
                        } else {
                            value = handler.getValue(prop);
                        }
                    }
                }
                // check if number of handlers for property has changed
                if (provides != lastProvides[prop.ordinal()]) {
                    if (lastProvides[prop.ordinal()] < 0) {
                        if ((provides != 1) && (prop != SourceDataEnum.PAUSE)) {
                            logger.warn("Number of handlers providing " + prop
                                    + " is " + provides);
                        }
                    } else {
                        logger.warn("Number of handlers providing " + prop
                                + " changed " + lastProvides[prop.ordinal()] + "->" + provides);
                    }
                    lastProvides[prop.ordinal()] = provides;
                }
                // and set value in the telemetry
                switch (prop) {
                    case WHEEL_SPEED:
                        t.setWheelSpeed(value);
                        break;
                    case CADENCE:
                        t.setCadence((int)value);
                        break;
                    case HEART_RATE:
                        t.setHeartRate((int)value);
                        break;
                    case POWER:
                        t.setPower((int)value);
                        break;
                    case RESISTANCE:
                        t.setResistance((int)value);
                        break;
                    case SLOPE:
                        t.setGradient(value);
                        break;
                    case ALTITUDE:
                        t.setElevation(value);
                        break;
                    case LATITUDE:
                        t.setLatitude(value);
                        break;
                    case LONGITUDE:
                        t.setLongitude(value);
                        break;
                    case SPEED:
                        t.setSpeed(value);
                        break;
                    case PAUSE:
                        t.setPaused((int)value);
                        break;
                    default:
                        throw new UnsupportedOperationException("Telemetry provider doesn't handle " + prop);
                }
            }
            t.setTime(runtime);
            t.setDistance(distance);
            MessageBus.INSTANCE.send(Messages.TELEMETRY, t);

            // sleep some time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                break;
            }

            // advance time and distance if not paused
            long timePassed = System.currentTimeMillis() - start; // [ms]
            // advance ridden distance and time, only if not paused
            // (this is: no pause message is defined)
            if (pauseMsg(t) == null) {
                distance += t.getSpeed() * (timePassed / 1000.0) / 3600.0;
                runtime += timePassed;
            }
        }
        logger.debug("TelemetryProvider stopped");
    }

    @Override
    public void callback(Messages m, Object o) {
        switch (m) {
            case START:
                if (runner == null) {
                    logger.debug("TelemetryProvider start requested");
                    runner = new Thread() {
                        @Override
                        public void run() {
                            threadLoop();
                        }
                    };
                    runner.start();
                } else {
                    logger.error("TelemetryProvider already started");
                }
                break;
            case STOP:
                // ask for save if distance bigger than 0
                if (runner != null) {
                    logger.debug("TelemetryProvider stop requested");
                    runner.interrupt();
                    runner = null;
                } else {
                    logger.error("TelemetryProvider not started");
                }
                break;

            case SUBSYSTEM:
                // check if subsystem already added
                for (SubsystemIntf existing : subsystems) {
                    // compare references, not objects!
                    if (existing == o) {
                        return;
                    }
                }
                logger.debug("TelemetryProvider subsystem " + ((SubsystemIntf) o).getType() + " created");
                subsystems.add((SubsystemIntf) o);
                break;
            case SUBSYSTEM_REMOVED:
                logger.debug("TelemetryProvider subsystem " + ((SubsystemIntf) o).getType() + " removed");
                subsystems.remove((SubsystemIntf) o);
                break;

            case HANDLER:
                // check if subsystem already added
                for (SourceDataProcessorIntf existing : handlers) {
                    // compare references, not objects!
                    if (existing == o) {
                        return;
                    }
                }
                logger.debug("TelemetryProvider handler " + ((SourceDataProcessorIntf) o).getPrettyName()+ " created");
                // add handler and notify about all available subsystems
                handlers.add((SourceDataProcessorIntf) o);
                if (o instanceof MessageCallback) {
                    if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM, (MessageCallback) o)) {
                        for (SubsystemIntf subsystem: subsystems) {
                            ((MessageCallback) o).callback(Messages.SUBSYSTEM, (Object) subsystem);
                        }
                    }
                }
                break;
            case HANDLER_REMOVED:
                logger.debug("TelemetryProvider handler " + ((SourceDataProcessorIntf) o).getPrettyName()+ " removed");
                handlers.remove((SourceDataProcessorIntf) o);
                if (o instanceof MessageCallback) {
                    if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM_REMOVED, (MessageCallback) o)) {
                        for (SubsystemIntf subsystem: subsystems) {
                            ((MessageCallback) o).callback(Messages.SUBSYSTEM_REMOVED, (Object) subsystem);
                        }
                    }
                }
                break;


            case STARTPOS:
                logger.debug("TelemetryProvider position " + (Double) o);
                distance = (Double) o;
                break;
            case GPXLOAD:
                logger.debug("TelemetryProvider file load");
                distance = 0.0;
                break;
            case CLOSE:
                logger.debug("TelemetryProvider file closed");
                distance = 0.0;
                break;
        }
    }
}
