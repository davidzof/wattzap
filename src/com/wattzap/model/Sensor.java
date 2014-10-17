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

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;

/**
 * General class acting as a sensor. It hanldes "connected" subsystem, sensor id
 * for configField editor and general behaviour of setting the data with validity.
 * All derived classes must handle configChanged and subsystemState callbacks
 * properly.
 * @author Jarek
 */
public abstract class Sensor extends SourceDataHandler
    implements MessageCallback, SensorIntf
{
    protected final long[] modifications = new long[SourceDataEnum.values().length];
    private int sensorId; // synchronized
    private SubsystemIntf subsystem = null;

    public Sensor() {
        // initialize all values to not modified
        for (int i = 0; i < modifications.length; i++) {
            modifications[i] = 0;
        }
    }

    @Override
    protected void setValue(SourceDataEnum data, double value) {
        long current = System.currentTimeMillis();
        super.setValue(data, value);
        synchronized(this) {
            modifications[data.ordinal()] = current;
        }
    }
    @Override
    public long getModificationTime(SourceDataEnum data) {
        synchronized(this) {
            return modifications[data.ordinal()];
        }
    }

    @Override
    protected long setLastMessageTime() {
        return setLastMessageTime(System.currentTimeMillis());
    }

    @Override
    public SourceDataHandlerIntf initialize() {
        // message registration
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.register(Messages.SUBSYSTEM_REMOVED, this);

        // initialize configuration of the sensor
        configChanged(UserPreferences.INSTANCE);

        // notify TelemetryProvider about new handler
        MessageBus.INSTANCE.send(Messages.HANDLER, this);

        // will receive SUBSYSTEM notification in a second
        setSensorId(UserPreferences.INSTANCE.getSensorId(getPrettyName()));

        return this;
    }

    @Override
    public void release() {
        MessageBus.INSTANCE.unregister(Messages.CONFIG_CHANGED, this);
        MessageBus.INSTANCE.unregister(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.unregister(Messages.SUBSYSTEM_REMOVED, this);

        // sensor not ready anymore
        setLastMessageTime(0);

        // request handler removal
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        MessageBus.INSTANCE.send(Messages.HANDLER_REMOVED, this);
    }

    @Override
    public SubsystemIntf getSubsystem() {
        return subsystem;
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

    @Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                UserPreferences property = (UserPreferences) o;
                if ((property == UserPreferences.SENSORS) && (getPrettyName().equals(property.getString()))) {
                    setSensorId(property.getSensorId(getPrettyName()));
                }
                configChanged(property);
                break;
            case SUBSYSTEM:
                if (((SubsystemIntf) o).getType() == getSubsystemType()) {
                    if (subsystem == null) {
                        subsystem = (SubsystemIntf) o;
                    } else if (subsystem != o) {
                        System.err.println("Different subsystem of type " +
                                getSubsystemType() + " found?!?!");
                        return;
                    }
                    if (subsystem.isOpen()) {
                        subsystemState(true);
                    } else {
                        subsystemState(false);
                        setLastMessageTime(0);
                    }
                }
                break;
            case SUBSYSTEM_REMOVED:
                if (subsystem == (SubsystemIntf) o) {
                    subsystemState(false);
                    setLastMessageTime(0);
                    // subsystem not available anymore
                    subsystem = null;
                }
                break;
        }
    }
    public abstract void subsystemState(boolean enabled);

}
