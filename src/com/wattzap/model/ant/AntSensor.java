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
import com.wattzap.model.Sensor;
import com.wattzap.model.SubsystemTypeEnum;
import com.wattzap.model.UserPreferences;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;

/**
 *
 * @author Jarek
 */
public abstract class AntSensor extends Sensor
    implements AntSensorIntf, BroadcastListener<BroadcastDataMessage>
{
	private static final Logger logger = LogManager.getLogger("Sensor");

    private Channel channel = null;

    @Override
    public void configChanged(UserPreferences config) {
    }

    @Override
    public SubsystemTypeEnum getSubsystemType() {
        return SubsystemTypeEnum.ANT;
    }

    // handling received message data
    abstract public void storeReceivedData(long time, int[] data);

    @Override
	public void receiveMessage(BroadcastDataMessage message) {
        // sensor just stopped while message received.. ignore it!
        if (channel == null) {
            return;
        }

        if (setLastMessageTime() == 0) {
            logger.debug(toString() + ":: first message received");
            if (getSensorId() == 0) {
                // if sensorId is a mask, just get real value and update
                // configuration when new sensorId is received, notification
                // about sensor ready is sent.
                new AntSensorIdQuery(this, channel).start();
            } else {
                // imediatelly send notification about sensor ready
                MessageBus.INSTANCE.send(Messages.HANDLER, this);
            }
        }

        int[] data = message.getUnsignedData();
        storeReceivedData(getLastMessageTime(), data);
    }

    @Override
    public void setSensorId(int sensorId) {
        synchronized(this) {
            // channel is recreated only if non-zero id is replaced with another
            // non-zero id..
            if ((channel != null) && (getSensorId() != 0) && (getSensorId() != sensorId)) {
                Channel chn = channel;
                channel = null;
                logger.debug("Restart channel for " + getPrettyName() + "." + sensorId);
                ((AntSubsystemIntf) getSubsystem()).closeChannel(chn);
                setLastMessageTime(0);
                assert getSubsystem() != null : "Subsystem doesn't exist";
                super.setSensorId(sensorId);
                channel = ((AntSubsystemIntf) getSubsystem()).createChannel(this);
            } else {
                // otherwise just store new id..
                super.setSensorId(sensorId);
            }
        }
    }

    @Override
    public void subsystemState(boolean enabled) {
        if (enabled) {
            assert (channel == null) : "Channel already created";
            channel = ((AntSubsystemIntf) getSubsystem()).createChannel(this);
        } else {
            if (channel != null) {
                logger.debug("Close channel for " + getPrettyName() + ":" +
                    getTransmissionType() + "." + getSensorId());
                ((AntSubsystemIntf) getSubsystem()).closeChannel(channel);
                channel = null;
            }
        }
    }

    @Override
    public void handleChannelId(Channel channel, int sensorId) {
        // if response is from requested channel..
        if ((this.channel == channel) && (getSensorId() == 0)) {
            if (((sensorId >> 16) & 0xf) != getTransmissionType()) {
                logger.error(toString() + ":: Incorrect transmission type " +
                        ((sensorId >> 16) & 0xf));
                return;
            }
            // store channel configuration
            setSensorId((sensorId & 0xffff) | ((sensorId >> 4) & 0xf0000));
            // this call configChanged callback
            UserPreferences.INSTANCE.setSensorId(getPrettyName(), getSensorId());
            MessageBus.INSTANCE.send(Messages.HANDLER, this);
        }
    }

    @Override
    public String toString() {
        return getPrettyName() + ":" + getTransmissionType() + "." + getSensorId();
    }
}
