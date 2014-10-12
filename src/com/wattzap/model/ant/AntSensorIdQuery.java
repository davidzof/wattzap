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

import org.cowboycoders.ant.Channel;

/**
 *
 * @author Jarek
 */
// channelId must be obtained outside USB thread! Otherwise response
// never returns back. Don't struggle with changing in parallel from
// config panel and from here.
public class AntSensorIdQuery extends Thread {
    private final AntSubsystemIntf subsystem;
    private final AntSensorIntf sensor;
    private final Channel channel;

    public AntSensorIdQuery(AntSensorIntf sensor, Channel channel) {
        this.subsystem = (AntSubsystemIntf) sensor.getSubsystem();
        this.sensor = sensor;
        this.channel = channel;
        assert sensor.getPrettyName() != null : "Sensor without a name";
    }

    @Override
    public void run() {
        sensor.handleChannelId(channel, subsystem.getChannelId(channel));
    }
}
