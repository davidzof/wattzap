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
package com.wattzap.model.ant;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.UserPreferences;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.cowboycoders.ant.Channel;

/**
 *
 * @author Jarek
 */
// channelId must be obtained outside USB thread! Otherwise response
// never returns back. Don't struggle with changing in parallel from
// config panel and from here.
public class AntSensorIdQuery extends Thread {
    private static Logger logger = LogManager.getLogger("SensorId");

    private final AntSubsystemIntf subsystem;
    private final AntSensorIntf sensor;
    private final Channel channel;
    private final long startTime;

    public AntSensorIdQuery(AntSubsystemIntf subsystem, AntSensorIntf sensor, Channel channel) {
        this.subsystem = subsystem;
        this.sensor = sensor;
        this.channel = channel;
        startTime = System.currentTimeMillis();
        logger.debug("AntSensor ID query created for " + sensor.getPrettyName());
    }

    @Override
    public void start() {
        logger.debug("AntSensor ID query started for " + sensor.getPrettyName());
        super.start();
    }

    @Override
    public void run() {
        logger.debug("AntSensor ID query update sensor ID for " + sensor.getPrettyName());
        int sId = subsystem.getChannelId(channel);
        logger.debug("Received " + sId + " after " + (System.currentTimeMillis() - startTime) + "for " + sensor.getPrettyName());

        // replace only if 0 is still set (this is value was not changed from GUI)
        if (sensor.getSensorId() == 0) {
            sensor.setSensorId(sId);
            // this should call configChanged callback
            UserPreferences.INSTANCE.setSensorId(sensor.getSensorName(), sId);
            MessageBus.INSTANCE.send(Messages.HANDLER, sensor);
        }
    }
}
