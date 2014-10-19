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

/**
 * Declaration for source data handler.
 *
 * Handlers are three kinds: these who can read from real devices (like S&C sensors,
 * trainer), virtual devices (like keyboard do change trainer resistance or
 * to pause training) and these who are able to compute data from previous
 * telemetry record.
 *
 * All virtual devices and telemetry computations are created in main application,
 * just when TelemetryProvider is ready.
 * Next sensorSubsystems are created, and all sensorHandlers are created. Selected
 * sensors (speed/cadence/heart rate/power) are selected as default source of data.
 * Everything is done on configuration data.
 * When subsystems are started, all their handlers initialize (on callback) and
 * are ready to work.
 * If any "important" subsystem is not started, time doesn't advance (because of
 * "pause" mode), and must be enabled and all handlers initialized (and connections
 * must be made, as well).
 *
 * @author Jarek
 */
public interface SourceDataHandlerIntf
{
    // initialize handler: register messages, fill internal data, etc
    SourceDataHandlerIntf initialize();
    // un-initialize hander: unregister messages, stop channels, etc
    void release();

    /* Name of the handler, used in configuration panel */
    String getPrettyName();
    void setPrettyName(String name);

    /* activate telemetryHandler */
    void setActive(boolean active);

   /* Which data handler provides. If handler provides these data, it is
    * available in related source selector. If not, it doesn't deliver the
    * value at all (throws exception?).
    * It shall be used for non-paired ANT+ sensors.
    */
   boolean provides(SourceDataEnum data);

   /* Access to handled data. */
   double getValue(SourceDataEnum data);

   /* Sensor: get last change time of the value. Time > than 5s means no updates
    * and indicator becomes orange. Value shown in ODO is grayed as well.
    * Telemetry handler: check current value (if too big or too little). Negative
    * value means too_little, positive too_big, zero value meets criteria. In
    * ODO these values are indicated as red (too little) or blue (too big).
    */
   long getModificationTime(SourceDataEnum data);

   /* Sensor: Get last just (handled) message time. If ==0, no messages were
    * received at all (and sensor indicator is gray), if any message was read
    * before 10s sensor indicator becomes red..
    * Otherwise indicator show orange (value not updated for last 5 seconds) or
    * green (value updated recently).
    * Telemetry handler: If it returns 0, handler is not active. No data is
    * taken from this handler. -1 value means handler is active and provides
    * all the data.
    */
   long getLastMessageTime();
}
