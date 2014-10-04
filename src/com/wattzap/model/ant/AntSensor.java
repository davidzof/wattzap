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
import com.wattzap.model.SubsystemIntf;
import com.wattzap.model.SubsystemTypeEnum;
import com.wattzap.model.SourceDataProcessorIntf;
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
	private static Logger logger = LogManager.getLogger("Sensor");

    /* sensorId might be changed when paired */
    private int sensorId; // synchronized
    private Channel channel = null;
    private AntSubsystemIntf subsystem = null;


    @Override
    public SourceDataProcessorIntf initialize() {
        setSensorId(UserPreferences.INSTANCE.getSensorId(getSensorName()));
        return super.initialize();
    }

    @Override
    public int getSensorId() {
        synchronized (this) {
            return sensorId;
        }
    }

    @Override
    public void setSensorId(int sId) {
        synchronized (this) {
            sensorId = sId;
        }
    }

    // handling received message data
    abstract public void storeReceivedData(long time, int[] data);

    @Override
	public void receiveMessage(BroadcastDataMessage message) {
        if (setLastMessageTime() == 0) {
            logger.debug("First " + getPrettyName() + " message received, sensorId=" + getSensorId());
            if (getSensorId() == 0) {
                // if sensorId is a mask, just get real value and update configuration
                // when new sensorId is received, notification about sensor ready is sent
                new AntSensorIdQuery(subsystem, this, channel).start();
            } else {
                // imediatelly send notification about sensor ready
                MessageBus.INSTANCE.send(Messages.HANDLER, this);
            }
        }

        int[] data = message.getUnsignedData();
        storeReceivedData(getLastMessageTime(), data);
    }

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                logger.debug("Configuration changed for " + getPrettyName() + " #" + getSensorId());
                // if sensorId was changed, it might be important to restart the channel
                if (UserPreferences.INSTANCE.getSensorId(getSensorName()) != getSensorId()) {
                    logger.debug("Sensor id changed to " + UserPreferences.INSTANCE.getSensorId(getSensorName()));
                    setSensorId(UserPreferences.INSTANCE.getSensorId(getSensorName()));
                    // if channel exist.. just recreate it in order to get proper messages.
                    if (channel != null) {
                        logger.debug("Restart channel for " + getPrettyName() + " #" + getSensorId());
                        subsystem.closeChannel(channel);
                        setLastMessageTime(0);
                        channel = subsystem.createChannel(getSensorId(), this);
                    }
                }
                configChanged(UserPreferences.INSTANCE);
                break;

            case SUBSYSTEM:
                if (((SubsystemIntf) o).getType() == SubsystemTypeEnum.ANT) {
                    if (subsystem == null) {
                        subsystem = (AntSubsystemIntf) o;
                    } else if (subsystem != o) {
                        logger.error("Different subsystem of type ANT found?!?!");
                        return;
                    }
                    if (subsystem.isOpen()) {
                        if (channel == null) {
                            logger.debug("Subsystem started, create channel for " + getSensorId());
                            channel = subsystem.createChannel(sensorId, this);
                        }
                    } else {
                        if (channel != null) {
                            logger.debug("Subsystem stopped, close channel for " + getSensorId());
                            subsystem.closeChannel(channel);
                            channel = null;
                            setLastMessageTime(0);
                        }
                    }
                }
                break;
            case SUBSYSTEM_REMOVED:
                if (subsystem == (AntSubsystemIntf) o) {
                    logger.debug("ANT Subsystem removed");
                    if (channel != null) {
                        logger.debug("Subsystem removed, close channel for " + getSensorId());
                        subsystem.closeChannel(channel);
                        channel = null;
                        setLastMessageTime(0);
                    }
                    subsystem = null;
                }
                break;
        }
    }
}
