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
public class TelemetryProvider implements MessageCallback
{
	private final Logger logger = LogManager.getLogger("Telemetry");

    // existing sensor subsystems, currently only single ANT+..
    private final List<SubsystemIntf> subsystems = new ArrayList<>();

    // list of handlers for source data (read from subsystems, or computed on telemetry..)
    private final List<SourceDataProcessorIntf> handlers = new ArrayList<>();
    private final Map<SourceDataEnum, SourceDataProcessorIntf> selectedHandlers = new HashMap<>();
    private final SourceDataProcessorIntf dummyHandler = new DummyProcessor();

    /* current "location" and training time, filled in telemetry to be reported */
    private Telemetry t = new Telemetry();
    private double distance = 0.0; // [m]
    private long runtime = 0; // [ms]
    private double speed = 0.0; // [m/s]
    private boolean paused = true;

    private Thread runner = null;

    public void initialize() {
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM_REMOVED, this);
        MessageBus.INSTANCE.register(Messages.HANDLER, this);
        MessageBus.INSTANCE.register(Messages.HANDLER_REMOVED, this);
        MessageBus.INSTANCE.register(Messages.STARTPOS, this);
        MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
        MessageBus.INSTANCE.register(Messages.CLOSE, this);
        MessageBus.INSTANCE.register(Messages.START, this);
        MessageBus.INSTANCE.register(Messages.STOP, this);
    }

    /* Subsystems and handlers, used by ConfigurationPanel only.. */

    // subsystems to be handled
    public List<SubsystemIntf> getSubsystems() {
        return subsystems;
    }
    // all registered handlers
    public List<SourceDataProcessorIntf> getSourceDataHandlers() {
        return handlers;
    }
    // selected data handlers
    public SourceDataProcessorIntf getSourceDataHanlder(SourceDataEnum data) {
        return selectedHandlers.get(data);
    }
    public void setSourceDataHanlder(SourceDataEnum data, SourceDataProcessorIntf handler) {
        selectedHandlers.put(data, handler);
    }

    private double getValue(SourceDataEnum data, long currentTime) {
        SourceDataProcessorIntf handler = selectedHandlers.get(data);
        if (handler == null) {
            handler = dummyHandler;
        }
        if ((currentTime > 0) && (handler.getModificationTime(data) < currentTime - 5000)) {
            paused = true;
        }
        return handler.getValue(data);
    }
    private double getValue(SourceDataEnum data) {
        SourceDataProcessorIntf handler = selectedHandlers.get(data);
        if (handler == null) {
            handler = dummyHandler;
        }
        return handler.getValue(data);
    }

    /* Main loop: get all data, process it and send current telemetry, then sleep some time.
     * Advance time and distance as well.
     * Next step.. set indicators
     */
    private void threadLoop() {
        logger.debug("TelemetryProvider started");
        while (!Thread.currentThread().isInterrupted()) {
            long start = System.currentTimeMillis();
            paused = false;
            speed = getValue(SourceDataEnum.SPEED, start);

            // fill telemetry to send
            t.setTime(runtime);
            t.setDistance(distance);
            t.setSpeed(speed);
            t.setPower((int) getValue(SourceDataEnum.POWER, start));
            t.setWheelSpeed(getValue(SourceDataEnum.WHEEL_SPEED, start));
            t.setCadence((int)getValue(SourceDataEnum.CADENCE));
            t.setHeartRate((int)getValue(SourceDataEnum.HEART_RATE));
            t.setGradient(getValue(SourceDataEnum.SLOPE));
            t.setElevation(getValue(SourceDataEnum.ALTITUDE));
            t.setLatitude(getValue(SourceDataEnum.LATITUDE));
            t.setLongitude(getValue(SourceDataEnum.LONGITUDE));
            t.setResistance((int)getValue(SourceDataEnum.RESISTANCE));
            t.setAutoResistance((int)getValue(SourceDataEnum.AUTO_RESISTANCE) != 0);
            t.setPaused(((int)getValue(SourceDataEnum.PAUSE) != 0) || paused);

            // TODO what about constraints? Shall they be included in telemetry?
            // I don't like the idea of "special" source of such information..

            MessageBus.INSTANCE.send(Messages.SPEEDCADENCE, t);

            // sleep some time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                break;
            }

            // advance time and distance if not paused
            long timePassed = System.currentTimeMillis() - start; // [ms]
            // advance ridden distance and time, only if pause condition not detected
            if (!paused) {
                distance += speed * (timePassed / 1000.0);
                runtime += timePassed;
            }
        }
        logger.debug("TelemetryProvider stopped");
    }

    @Override
    public void callback(Messages m, Object o) {
        SourceDataProcessorIntf handler;
        switch (m) {
            case START:
                logger.debug("TelemetryProvider start requested");
                runner = new Thread() {
                    @Override
                    public void run() {
                        threadLoop();
                    }
                };
                break;
            case STOP:
                logger.debug("TelemetryProvider stop requested");
                // ask for save if distance bigger than 0
                if (runner != null) {
                    runner.interrupt();
                    runner = null;
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
                speed = 0.0;
                break;
            case GPXLOAD:
                logger.debug("TelemetryProvider file load");
                distance = 0.0;
                speed = 0.0;
                break;
            case CLOSE:
                logger.debug("TelemetryProvider file closed");
                distance = 0.0;
                speed = 0.0;
                break;
        }
    }
}
