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
import com.wattzap.model.SensorSubsystem;
import com.wattzap.model.SensorSubsystemTypeEnum;
import com.wattzap.model.SourceDataHandlerAbstract;
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
    private AntSensorSubsystem subsystem = null;

    public AntSourceDataHandler(int sensorId) {
        this.sensorId = sensorId;

        // handler not initialized yet
        lastMessageTime = 0;
        // notify TelemetryProvider about new handler
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        // will receive SUBSYSTEM notification in a second
    }

    // default sensor configuration
    abstract public String getSensorName();
    abstract public int getSensorType();
    abstract public int getSensorFrequency();
    abstract public int getSensorPeriod();

    // handling received message data
    abstract public void storeReceivedData(int[] data);

    @Override
	public void receiveMessage(BroadcastDataMessage message) {
        boolean reportNew = (lastMessageTime == 0);
        lastMessageTime = System.currentTimeMillis();

        if (reportNew) {
            MessageBus.INSTANCE.send(Messages.HANDLER, this);
            if (sensorId == 0) {
                // change configuration of sensor: it must be done automatically!
                /*
                propertyPage.removeSensor(getName());
                */
                sensorId = subsystem.getChannelId(channel);
                /*
                propertyPage.addSensor(getName(), sensorId);
                */
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
            }
        }

        int[] data = message.getUnsignedData();
        storeReceivedData(data);
    }


	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                // get the properties..
                break;

            case SUBSYSTEM:
                if (((SensorSubsystem) o).getType() == SensorSubsystemTypeEnum.ANT) {
                    subsystem = (AntSensorSubsystem) o;
                    if (subsystem.isOpen()) {
                        channel = subsystem.createChannel(sensorId, this);
                    }
                }
                break;

            case SUBSYSTEM_REMOVED:
                if (o == subsystem) {
                    subsystem.closeChannel(channel);
                    channel = null;
                    // not paired anymore..
                    lastMessageTime = 0;
                }
                break;
        }
    }
}
