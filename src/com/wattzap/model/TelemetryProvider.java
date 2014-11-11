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
import com.wattzap.model.dto.TelemetryValidityEnum;
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
    private final Map<SourceDataEnum, SourceDataHandlerIntf> selectedHandlers = new HashMap<>();

    /* current "location" and training time, filled in telemetry to be reported */
    private Telemetry t = null;
    private double distance = 0.0; // [km]
    private long runtime = 0; // [ms]

    private Thread runner = null;

    @Override
    public String toString() {
        return "TelemetryProvider:: ss=" + subsystems + ", handlers=" + handlers;
    }

    public TelemetryProvider initialize() {
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM_REMOVED, this);
        MessageBus.INSTANCE.register(Messages.HANDLER, this);
        MessageBus.INSTANCE.register(Messages.HANDLER_REMOVED, this);
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
        MessageBus.INSTANCE.register(Messages.STARTPOS, this);
        MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
        MessageBus.INSTANCE.register(Messages.CLOSE, this);
        MessageBus.INSTANCE.register(Messages.START, this);
        MessageBus.INSTANCE.register(Messages.STOP, this);
        MessageBus.INSTANCE.register(Messages.ROUTE_MSG, this);

        // set selected handlers
        configChanged(UserPreferences.INSTANCE);
        return this;
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
    public SourceDataHandlerIntf getSelected(SourceDataEnum data) {
        return selectedHandlers.get(data);
    }

    private class HandlerToReplace implements SourceDataHandlerIntf {
        private final String name;

        public HandlerToReplace(String name) {
            this.name = name;
        }

        @Override
        public SourceDataHandlerIntf initialize() {
            return this;
        }

        @Override
        public void release() {
        }

        @Override
        public String getPrettyName() {
            return name;
        }

        @Override
        public void setPrettyName(String name) {
            assert false : "Not allowed";
        }

        @Override
        public boolean provides(SourceDataEnum data) {
            return false;
        }

        @Override
        public double getValue(SourceDataEnum data) {
            assert false : "Not allowed";
            return 0.0;
        }

        @Override
        public boolean checks(SourceDataEnum data) {
            return false;
        }

        @Override
        public long getModificationTime(SourceDataEnum data) {
            return 0;
        }

        @Override
        public long getLastMessageTime() {
            return 0;
        }
    }

    private void setSensor(SourceDataEnum data, UserPreferences pref) {
        String name = pref.getString();
        selectedHandlers.put(data, new HandlerToReplace(name));
        for (SourceDataHandlerIntf handler : handlers) {
            if (name.equals(handler.getPrettyName())) {
                selectedHandlers.put(data, handler);
            }
        }
        if (selectedHandlers.get(data) == null) {
            selectedHandlers.put(data, new HandlerToReplace(pref.getString()));
        }
    }

    private void configChanged(UserPreferences pref) {
        if ((pref == UserPreferences.INSTANCE) ||
                (pref == UserPreferences.POWER_SOURCE)) {
            setSensor(SourceDataEnum.POWER, UserPreferences.POWER_SOURCE);
        }
        if ((pref == UserPreferences.INSTANCE) ||
                (pref == UserPreferences.SPEED_SOURCE)) {
            setSensor(SourceDataEnum.WHEEL_SPEED, UserPreferences.SPEED_SOURCE);
        }
    }

    public void setDistanceTime(double distance, long time) {
        this.distance = distance;
        this.runtime = time;
        MessageBus.INSTANCE.send(Messages.STARTPOS, (Double) distance);
    }

    /* Main loop: get all data, process it and send current telemetry, then sleep some time.
     * Advance time and distance as well.
     * Next step.. set indicators
     */
    private void threadLoop() {
        // initialize the number of handlers to be displayed. By default
        // expected is one handler for the property, DISTANCE and TIME should
        // not have any handlers.
        int[] lastHandlersNum = new int[SourceDataEnum.values().length];
        for (int i = 0; i < lastHandlersNum.length; i++) {
            lastHandlersNum[i] = -1;
        }
        lastHandlersNum[SourceDataEnum.DISTANCE.ordinal()] = 0;
        lastHandlersNum[SourceDataEnum.TIME.ordinal()] = 0;

        // send "dummy" telemetry, without any data except time and position.
        // it would be filled in a few seconds.
        if (t == null) {
            t = new Telemetry(PauseMsgEnum.INITIALIZE);
            t.setDistance(distance);
            t.setTime(runtime);
        } else {
            t.setPause(PauseMsgEnum.INITIALIZE);
        }
        MessageBus.INSTANCE.send(Messages.TELEMETRY, t);

        // Wait all handlers reinitialize to show what is wrong with configuration.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // if start discarded.. just break telemetry processing
            Thread.currentThread().interrupt();
        }

        // telemetry filled by sensors and handlers
        while (!Thread.currentThread().isInterrupted()) {
            long start = System.currentTimeMillis();
            int pause = 0;

            for (SourceDataEnum prop : SourceDataEnum.values()) {
                TelemetryValidityEnum validity = TelemetryValidityEnum.NOT_PRESENT;
                double value = prop.getDefault();

                switch (prop) {
                    case TIME:
                        validity = TelemetryValidityEnum.OK;
                        value = runtime;
                        break;
                    case DISTANCE:
                        validity = TelemetryValidityEnum.OK;
                        value = distance;
                        break;
                    case PAUSE:
                        continue;
                }

                SourceDataHandlerIntf selected = null;
                if (selectedHandlers.containsKey(prop)) {
                    // get data from selected handler
                    selected = getSelected(prop);
                    // set pause if selected handler was removed or
                    // is not created yet (and will not be created at all)
                    if ((selected == null) || (!selected.provides(prop))) {
                        if (selected == null) {
                            logger.error("Selected " + prop + " already removed");
                        } else {
                            logger.error("Selected " + prop + "(" + selected.getPrettyName()
                                    + ") not created yet");
                        }
                        if (pause < 300) {
                            pause = 300;
                        }
                        continue;
                    }
                }
                int handlersNum = 0;
                for (SourceDataHandlerIntf handler :  handlers) {
                    // if telemetryHandler is active and provides property
                    if ((handler == selected) ||
                            (selected == null) && handler.provides(prop))
                    {
                        handlersNum++;
                        // data from sensor is not valid after 5 seconds
                        if ((handler.getLastMessageTime() < 0) ||
                            (handler.getModificationTime(prop) >= start - 5000))
                        {
                            validity = TelemetryValidityEnum.OK;
                            value = handler.getValue(prop);
                        } else {
                            validity = TelemetryValidityEnum.NOT_AVAILABLE;
                        }
                        if (handler.provides(SourceDataEnum.PAUSE)) {
                            int p = (int) handler.getValue(SourceDataEnum.PAUSE);
                            if (pause < p) {
                                pause = p;
                            }
                        }
                    }
                    // handler checks property, only telemetries do that!
                    // in sensors modificationTime is used to check time,
                    // and telemetryHandler report here value condition.
                    if (handler.checks(prop)) {
                        TelemetryValidityEnum target;
                        if (handler.getModificationTime(prop) < 0) {
                            target = TelemetryValidityEnum.TOO_SMALL;
                        } else if (handler.getModificationTime(prop) > 0) {
                            target = TelemetryValidityEnum.TOO_BIG;
                        } else {
                            target = TelemetryValidityEnum.OK;
                        }
                        switch (validity) {
                            case NOT_PRESENT:
                            case NOT_AVAILABLE:
                                break;
                            case OK:
                                validity = target;
                                break;
                            case TOO_BIG:
                            case TOO_SMALL:
                                if (validity != target) {
                                    validity = TelemetryValidityEnum.WRONG;
                                }
                                break;
                            case WRONG:
                                // stays wrong..
                                break;
                        }
                    }
                }

                // special cases.. some values if reported negative, they should
                // be removed from ODO
                switch (prop) {
                    case WHEEL_SPEED:
                    case SPEED:
                        if (value < 0.0) {
                            validity = TelemetryValidityEnum.NOT_PRESENT;
                            value = 0.0;
                        }
                        break;
                    case RESISTANCE:
                        if (value < 1.0) {
                            logger.error("Wrong resistance, set 1");
                            value = 1.0;
                        }
                }

                // set validity, time/distance might be checked as well ("promoted"
                // to too_small/too_big)
                t.setDouble(prop, value, validity);

                // check if number of handlers for property has changed
                if (handlersNum != lastHandlersNum[prop.ordinal()]) {
                    if (lastHandlersNum[prop.ordinal()] < 0) {
                        if ((handlersNum != 1) && (prop != SourceDataEnum.PAUSE)) {
                            logger.warn("Number of handlers providing " + prop
                                    + " is " + handlersNum);
                        }
                    } else {
                        logger.warn("Number of handlers providing " + prop
                                + " changed " + lastHandlersNum[prop.ordinal()]
                                + "->" + handlersNum);
                    }
                    lastHandlersNum[prop.ordinal()] = handlersNum;
                }
            }
            t.setPause(PauseMsgEnum.get(pause));
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
            // (this is: no pause message is defined for pause value)
            // Handle normal routes (with distance) and routes with
            // time [s] in distance if no speed is provided by any handler.
            // (this is.. in trainings with TRN files).
            if (PauseMsgEnum.msg(t) == null) {
                if (t.isAvailable(SourceDataEnum.SPEED)) {
                    distance += t.getSpeed() * (timePassed / 1000.0) / 3600.0;
                } else {
                    distance += timePassed / 1000.0;
                }
                runtime += timePassed;
            }
        }
        // stopped, show proper message with last values
        t.setPause(PauseMsgEnum.STOPPED);
        MessageBus.INSTANCE.send(Messages.TELEMETRY, t);
    }

    @Override
    public void callback(Messages m, Object o) {
        SourceDataHandlerIntf handler;
        switch (m) {
            case CONFIG_CHANGED:
                configChanged((UserPreferences) o);
                break;
            case START:
                if (runner == null) {
                    runner = new Thread() {
                        @Override
                        public void run() {
                            threadLoop();
                        }
                    };
                    runner.setName("TelemetryProvider");
                    runner.start();
                } else {
                    logger.error("TelemetryProvider already started");
                }
                break;
            case STOP:
                // ask for save if distance bigger than 0
                if (runner != null) {
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
                handler = (SourceDataHandlerIntf) o;
                // replace all fake-handlers with just reported
                for (SourceDataEnum val : selectedHandlers.keySet()) {
                    SourceDataHandlerIntf selected = selectedHandlers.get(val);
                    // selected handler is marked as fake when doesn't provide
                    // required data.
                    if ((selected != null) &&
                        (!selected.provides(val)) &&
                        (selected.getPrettyName().equals(handler.getPrettyName())))
                    {
                        selectedHandlers.put(val, handler);
                    }
                }
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
                handler = (SourceDataHandlerIntf) o;
                // remove selected handler
                for (SourceDataEnum val : selectedHandlers.keySet()) {
                    SourceDataHandlerIntf selected = selectedHandlers.get(val);
                    if ((selected != null) &&
                        (selected.getPrettyName().equals(handler.getPrettyName())))
                    {
                        selectedHandlers.put(val, null);
                    }
                }
                // remove handler from the list
                handlers.remove(handler);
                // and notify handler about subsystem removal, just for fun
                // (if sensor doesn't unregister from subsystem on removal)
                if (o instanceof MessageCallback) {
                    if (MessageBus.INSTANCE.isRegisterd(Messages.SUBSYSTEM_REMOVED, (MessageCallback) o)) {
                        for (SubsystemIntf subsystem: subsystems) {
                            ((MessageCallback) o).callback(Messages.SUBSYSTEM_REMOVED, (Object) subsystem);
                        }
                    }
                }
                break;


            case ROUTE_MSG:
                String msg = (String) o;
                System.err.println("RouteMsg:: " + msg);
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
