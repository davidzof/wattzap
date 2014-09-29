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
import com.wattzap.model.power.Power;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks all (added) dataHandlers and computes periodically telemetry on settings
 * basis (speed, cadence, heart rate, power, trainer load).
 * @author Jarek
 */
public class TelemetryProvider extends Thread
    implements MessageCallback
{
    // existing sensor subsystems, currently only single ANT+..
    private final List<SensorSubsystem> subsystems = new ArrayList<>();

    // list of handlers for source data (read from subsystems, or computed on telemetry..)
    private final List<SourceDataHandlerIntf> handlers = new ArrayList<>();
    private final Map<SourceDataEnum, SourceDataHandlerIntf> selectedHandlers = new HashMap<>();
    private SourceDataHandlerIntf dummyHandler = new SourceDataHandlerDummy();

    /* current "location" and training time, filled in telemetry to be reported */
    private Telemetry t = new Telemetry();
    private double distance = 0.0; // [m]
    private long runtime = 0; // [ms]
    private double speed = 0.0; // [m/s]
    private boolean paused = true;


    /* Subsystems and handlers, used by ConfigurationPanel only.. */

    // subsystems to be handled
    public List<SensorSubsystem> getSubsystems() {
        return subsystems;
    }
    // all registered handlers
    public List<SourceDataHandlerIntf> getSourceDataHandlers() {
        return handlers;
    }
    // selected data handlers
    public SourceDataHandlerIntf getSourceDataHanlder(SourceDataEnum data) {
        return selectedHandlers.get(data);
    }
    public void setSourceDataHanlder(SourceDataEnum data, SourceDataHandlerIntf handler) {
        selectedHandlers.put(data, handler);
    }

    private double getValue(SourceDataEnum data, long currentTime) {
        SourceDataHandlerIntf handler = selectedHandlers.get(data);
        if (handler == null) {
            handler = dummyHandler;
        }
        if ((currentTime > 0) && (handler.getModificationTime(data) < currentTime - 5000)) {
            paused = true;
        }
        return handler.getValue(data);
    }

	private final UserPreferences userPrefs = UserPreferences.INSTANCE;
	private Power powerProfile = userPrefs.getPowerProfile();
	private double weight = userPrefs.getTotalWeight();

    /* Main loop: get all data, process it and send current telemetry, then sleep some time.
     * Advance time and distance as well.
     * Next step.. set indicators
     */
    public void run() {
        for (;;) {
            paused = false;
            long start = System.currentTimeMillis();

            // "default" power is computed on last reported wheelSpeed, slope,
            // trainer resistance and total weight
			speed = getValue(SourceDataEnum.SPEED, 0);
            double power = getValue(SourceDataEnum.POWER, start);
            double slope = getValue(SourceDataEnum.SLOPE, 0);
            int resistance = (int)getValue(SourceDataEnum.RESISTANCE, 0);
            boolean manualPause = ((int)getValue(SourceDataEnum.PAUSE, 0) != 0);

            // fill telemetry to send
            t.setTime(runtime);
            t.setDistance(distance);
            t.setSpeed(speed);
            t.setPower((int) power);
            t.setWheelSpeed(getValue(SourceDataEnum.WHEEL_SPEED, start));
            t.setCadence((int)getValue(SourceDataEnum.CADENCE, 0));
            t.setHeartRate((int)getValue(SourceDataEnum.HEART_RATE, 0));
            t.setGradient(slope);
            t.setElevation(getValue(SourceDataEnum.ALTITUDE, 0));
            t.setLatitude(getValue(SourceDataEnum.LATITUDE, 0));
            t.setLongitude(getValue(SourceDataEnum.LONGITUDE, 0));
            t.setResistance((resistance < 0) ? -resistance : resistance);
            t.setAutoResistance(resistance < 0);
            t.setPaused(paused || manualPause);
			MessageBus.INSTANCE.send(Messages.SPEEDCADENCE, t);


            // sleep some time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // advance time and distance if not paused
            long timePassed = System.currentTimeMillis() - start; // [ms]
            // advance ridden distance and time, only if pause condition not detected
            if (!paused) {
                distance += speed * (timePassed / 1000.0);
                runtime += timePassed;
            }
        }
    }

    @Override
    public void callback(Messages m, Object o) {
        SourceDataHandlerIntf handler;
        switch (m) {
            case SUBSYSTEM:
                // check if subsystem already added
                for (SensorSubsystem existing : subsystems) {
                    // compare references, not objects!
                    if (existing == o) {
                        return;
                    }
                }
                subsystems.add((SensorSubsystem) o);
                break;
            case SUBSYSTEM_REMOVED:
                subsystems.remove((SensorSubsystem) o);
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
                handler = (SourceDataHandlerIntf) o;
                handlers.add(handler);
                if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM, handler)) {
                    for (SensorSubsystem subsystem: subsystems) {
                        handler.callback(Messages.SUBSYSTEM, (Object) subsystem);
                    }
                }
                break;
            case HANDLER_REMOVED:
                handler = (SourceDataHandlerIntf) o;
                handlers.remove(handler);
                if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM_REMOVED, handler)) {
                    for (SensorSubsystem subsystem: subsystems) {
                        handler.callback(Messages.SUBSYSTEM_REMOVED, (Object) subsystem);
                    }
                }
                break;


            case STARTPOS:
                distance = (Double) o;
                speed = 0.0;
                break;
            case CLOSE:
                distance = 0.0;
                speed = 0.0;
                break;

        }

        // CLOSE, GPXLOAD
    }
}
