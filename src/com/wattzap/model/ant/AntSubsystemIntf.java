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

import com.wattzap.model.SubsystemIntf;
import org.cowboycoders.ant.Channel;

/**
 *
 * @author Jarek
 */
public interface AntSubsystemIntf extends SubsystemIntf {
    /* create new free channel (if available).
     * When channel is properly paired, new messages are passed to its handler.
     */
    Channel createChannel(AntSensor sensor);

    /* Get current sensorId for registered channel. Channel can be created with
     * exact ID or with 0 ("mask"). Return non-zero value if channel is paired.
     * AntSensor is passed to be added to
     */
    int getChannelId(Channel channel, AntSensorIntf sensor);

    /* close and free the channel. */
    void closeChannel(Channel channel);
}
