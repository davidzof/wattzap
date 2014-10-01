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
import com.wattzap.model.SensorSubsystemIntf;
import com.wattzap.model.SensorSubsystemTypeEnum;
import com.wattzap.model.SourceDataHandlerAbstract;
import com.wattzap.model.UserPreferences;
import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;

/**
 *
 * @author Jarek
 */
public abstract class AntSourceDataHandler extends SourceDataHandlerAbstract
    implements BroadcastListener<BroadcastDataMessage>
{
    /* sensorId might be changed when paired */
    private int sensorId;
    private Channel channel = null;
    private AntSensorSubsystemIntf subsystem = null;

    public AntSourceDataHandler() {
        // handler not initialized yet
        lastMessageTime = 0;
    }

    public void initialize() {
        // message registration
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM_REMOVED, this);

        // initialize configuration
        sensorId = UserPreferences.INSTANCE.getSensorId(getSensorName());
        configChanged(UserPreferences.INSTANCE);

        // notify TelemetryProvider about new handler
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        // will receive SUBSYSTEM notification in a second
    }

    public void release() {
        MessageBus.INSTANCE.unregister(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.unregister(Messages.SUBSYSTEM_REMOVED, this);

        // request handler removal
        lastMessageTime = 0;
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        MessageBus.INSTANCE.send(Messages.HANDLER_REMOVED, this);
    }

    // default sensor configuration
    abstract public String getSensorName();
    abstract public int getSensorType();
    abstract public int getSensorPeriod();

    // handling received message data
    abstract public void storeReceivedData(long time, int[] data);

    @Override
	public void receiveMessage(BroadcastDataMessage message) {
        boolean reportNew = (lastMessageTime == 0);
        lastMessageTime = System.currentTimeMillis();

        if (reportNew) {
            MessageBus.INSTANCE.send(Messages.HANDLER, this);
            if (sensorId == 0) {
                sensorId = subsystem.getChannelId(channel);
                UserPreferences.INSTANCE.setSensorId(getSensorName(), sensorId);
            }
        }

        int[] data = message.getUnsignedData();
        storeReceivedData(lastMessageTime, data);
    }

    public abstract void configChanged(UserPreferences config);

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                configChanged(UserPreferences.INSTANCE);
                break;

            case SUBSYSTEM:
                if (((SensorSubsystemIntf) o).getType() == SensorSubsystemTypeEnum.ANT) {
                    if (subsystem == null) {
                        subsystem = (AntSensorSubsystemIntf) o;
                    }
                    if (subsystem.isOpen()) {
                        if (channel == null) {
                            channel = subsystem.createChannel(sensorId, this);
                        }
                    } else {
                        if (channel != null) {
                            subsystem.closeChannel(channel);
                            lastMessageTime = 0;
                        }
                    }
                }
                break;
            case SUBSYSTEM_REMOVED:
                if (subsystem == (AntSensorSubsystemIntf) o) {
                    if (channel != null) {
                        subsystem.closeChannel(channel);
                        lastMessageTime = 0;
                    }
                    subsystem = null;
                }
                break;
        }
    }
}
