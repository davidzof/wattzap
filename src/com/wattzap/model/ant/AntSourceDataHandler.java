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
    private final int sensorType;
    private int sensorId;
    private Channel channel = null;

    public AntSourceDataHandler(int sensorType, int sensorId) {
        this.sensorType = sensorType;
        this.sensorId = sensorId;

        // handler not initialized yet
        lastMessageTime = 0;
        // notify TelemetryProvider about new handler
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
    }

    abstract public void storeValues(int[] data);

    @Override
	public void receiveMessage(BroadcastDataMessage message) {
        boolean reportNew = (lastMessageTime == 0);
        lastMessageTime = System.currentTimeMillis();
        if (reportNew) {
            MessageBus.INSTANCE.send(Messages.HANDLER, this);
            if (sensorId == 0) {
                // change configuration of sensor: it must be done automatically!
                propertyPage.removeSensor(getName());
                sensorId = 1234;
                propertyPage.addSensor(getName(), sensorId);
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
            }
        }

        int[] data = message.getUnsignedData();
        storeValues(data);
    }


	@Override
	public void callback(Messages message, Object o) {
        AntSensorSubsystem subsystem;
		switch (message) {
            case CONFIG_CHANGED:
                // get from properties..
                break;
            case SUBSYSTEM:
                if (((SensorSubsystem) o).getType()== SensorSubsystemTypeEnum.ANT) {
                    subsystem = (AntSensorSubsystem) o;
                    if (subsystem.isOpen()) {
                        channel = subsystem.createChannel(sensorType, sensorId);
                    }
                }
        }
    }
}
