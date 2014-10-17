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
    private final List<SourceDataHandlerIntf> handlers = new ArrayList<>();
    private final Map<SourceDataEnum, SensorIntf> sensors = new HashMap<>();

    /* current "location" and training time, filled in telemetry to be reported */
    private Telemetry t = null;
    private double distance = 0.0; // [km]
    private long runtime = 0; // [ms]

    private Thread runner = null;
    private int[] lastHandlersNum = new int[SourceDataEnum.values().length];

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
        pauseMsgKeys.put(-2, "stopped");
        pauseMsgKeys.put(-1, "initialize");
        // normal training condition
        pauseMsgKeys.put(0, null);
        pauseMsgKeys.put(1, "start_training");
        // speed is zero during normal training
        pauseMsgKeys.put(2, "no_movement");
        // pause button was pressed
        pauseMsgKeys.put(3, "manual_pause");

        // race preparation, wheelSpeed must be detected
        pauseMsgKeys.put(10, "race_prepare");
        // countdown
        pauseMsgKeys.put(20, "9");
        pauseMsgKeys.put(21, "8");
        pauseMsgKeys.put(22, "7");
        pauseMsgKeys.put(23, "6");
        pauseMsgKeys.put(24, "5");
        pauseMsgKeys.put(25, "4");
        pauseMsgKeys.put(26, "3");
        pauseMsgKeys.put(27, "2");
        pauseMsgKeys.put(28, "1");
        pauseMsgKeys.put(29, "race_start");
        // normal race condition, any pause is not allowed
        pauseMsgKeys.put(30, null);

        // end of training. Set by video handler, cannot be overriden
        pauseMsgKeys.put(100, "end_of_training");
    }
    public static String pauseMsg(Telemetry t, boolean nullIfUnknown) {
        String key = pauseMsgKeys.get(t.getPaused());
        if (key == null) {
            if (nullIfUnknown) {
                return null;
            } else {
                return String.format("unknown[%d]", t.getPaused());
            }
        }
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
    public List<SourceDataHandlerIntf> getHandlers() {
        return handlers;
    }

    // selected data handlers
    public SensorIntf getSensor(SourceDataEnum data) {
        return sensors.get(data);
    }
    public void setSensor(SourceDataEnum data, SourceDataHandlerIntf sensor) {
        if (sensor == null) {
            sensors.remove(data);
        } else {
            assert sensor instanceof SensorIntf :
                    "Non-sensor handler " + sensor.getPrettyName();
            sensors.put(data, (SensorIntf) sensor);
        }
    }

    /* Main loop: get all data, process it and send current telemetry, then sleep some time.
     * Advance time and distance as well.
     * Next step.. set indicators
     */
    private void threadLoop() {
        Thread.currentThread().setName("TelemetryProvider");
        for (int i = 0; i < lastHandlersNum.length; i++) {
            lastHandlersNum[i] = -1;
        }
        // send "dummy" telemetry, without any data except time and position.
        // it would be filled in a few seconds.
        t = new Telemetry();
        t.setDistance(distance);
        t.setTime(runtime);
        MessageBus.INSTANCE.send(Messages.TELEMETRY, t);
        // Wait all handlers reinitialize to show what is wrong with configuration.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // if start discarded.. just break telemetry processing
            Thread.currentThread().interrupt();
        }

        while (!Thread.currentThread().isInterrupted()) {
            long start = System.currentTimeMillis();

            for (SourceDataEnum prop : SourceDataEnum.values()) {
                double value = prop.getDefault();
                int handlersNum = 0;

                SensorIntf sensor = getSensor(prop);
                if ((sensor != null) && (sensor.provides(prop))) {
                    if (sensor.getModificationTime(prop) >= start - 5000) {
                        handlersNum++;
                        value = sensor.getValue(prop);
                    }
                }

                for (SourceDataHandlerIntf handler :  handlers) {
                    // if handler is active and provides property
                    if ((handler.getLastMessageTime() == -1) && (handler.provides(prop))) {
                        handlersNum++;
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
                if (handlersNum != lastHandlersNum[prop.ordinal()]) {
                    if (lastHandlersNum[prop.ordinal()] < 0) {
                        if ((handlersNum != 1) && (prop != SourceDataEnum.PAUSE)) {
                            logger.warn("Number of handlers providing " + prop
                                    + " is " + handlersNum);
                        }
                    } else {
                        logger.warn("Number of handlers providing " + prop
                                + " changed " + lastHandlersNum[prop.ordinal()] + "->" + handlersNum);
                    }
                    lastHandlersNum[prop.ordinal()] = handlersNum;
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
                        assert false : "Telemetry provider doesn't handle " + prop;
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
        // stopped, show proper message
        t.setPaused(-2);
        MessageBus.INSTANCE.send(Messages.TELEMETRY, t);
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
                subsystems.add((SubsystemIntf) o);
                break;
            case SUBSYSTEM_REMOVED:
                subsystems.remove((SubsystemIntf) o);
                break;

            case HANDLER:
                // check if subsystem already added
                for (SourceDataHandlerIntf existing : handlers) {
                    // compare references, not objects!
                    if (existing == o) {
                        return;
                    }
                }
                // add handler and notify about all available subsystems
                handlers.add((SourceDataHandlerIntf) o);
                if (o instanceof MessageCallback) {
                    if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM, (MessageCallback) o)) {
                        for (SubsystemIntf subsystem: subsystems) {
                            ((MessageCallback) o).callback(Messages.SUBSYSTEM, (Object) subsystem);
                        }
                    }
                }
                break;
            case HANDLER_REMOVED:
                // remove handler from the list
                handlers.remove((SourceDataHandlerIntf) o);
                if (o instanceof MessageCallback) {
                    if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM_REMOVED, (MessageCallback) o)) {
                        for (SubsystemIntf subsystem: subsystems) {
                            ((MessageCallback) o).callback(Messages.SUBSYSTEM_REMOVED, (Object) subsystem);
                        }
                    }
                }
                break;


            case STARTPOS:
                distance = (Double) o;
                break;
            case GPXLOAD:
                distance = 0.0;
                break;
            case CLOSE:
                distance = 0.0;
                break;
        }
    }
}
