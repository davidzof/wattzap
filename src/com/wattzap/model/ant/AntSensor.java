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
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.SubsystemIntf;
import com.wattzap.model.SubsystemTypeEnum;
import com.wattzap.model.SourceDataProcessor;
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
public abstract class AntSensor extends SourceDataProcessor
    implements AntSensorIntf, MessageCallback, BroadcastListener<BroadcastDataMessage>
{
	private static Logger logger = LogManager.getLogger("Sensor");

    /* sensorId might be changed when paired */
    private int sensorId; // synchronized
    private Channel channel = null;
    private AntSubsystemIntf subsystem = null;

    public AntSensor() {
        // handler not initialized yet
        setLastMessageTime(0);
    }

    @Override
    public void initialize() {
        // message registration
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM_REMOVED, this);

        // initialize configuration
        setSensorId(UserPreferences.INSTANCE.getSensorId(getSensorName()));
        configChanged(UserPreferences.INSTANCE);

        // notify TelemetryProvider about new handler
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        // will receive SUBSYSTEM notification in a second
    }

    @Override
    public void release() {
        MessageBus.INSTANCE.unregister(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.unregister(Messages.SUBSYSTEM_REMOVED, this);

        // request handler removal
        setLastMessageTime(0);
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        MessageBus.INSTANCE.send(Messages.HANDLER_REMOVED, this);
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
        boolean firstMessage = (setLastMessageTime() == 0);

        logger.debug("Receive message");
        if (firstMessage) {
            // sensor has just started
            MessageBus.INSTANCE.send(Messages.HANDLER, this);
            // if sensorId is a mask, just get real value and update configuration
            if (getSensorId() == 0) {
                new AntSensorIdQuery(subsystem, this, channel);
            }
        }

        logger.debug("Processing message");
        int[] data = message.getUnsignedData();
        storeReceivedData(getLastMessageTime(), data);
    }

    public abstract void configChanged(UserPreferences config);

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                // if sensorId was changed, it might be important to restart the channel
                if (UserPreferences.INSTANCE.getSensorId(getSensorName()) != getSensorId()) {
                    setSensorId(UserPreferences.INSTANCE.getSensorId(getSensorName()));
                    // if channel exist.. just recreate it in order to get proper messages.
                    if (channel != null) {
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
                        setLastMessageTime(0);
                    }
                    subsystem = null;
                }
                break;
        }
    }
}
