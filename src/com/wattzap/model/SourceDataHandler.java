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

/**
 *
 * @author Jarek
 */
public abstract class SourceDataHandler
    implements SourceDataHandlerIntf
{
    protected final double[] values = new double[SourceDataEnum.values().length];

    // has no valid information, cannot provide anything
    private long lastMessageTime = 0;
    private String prettyName;

    public SourceDataHandler() {
        // initialize all values to not modified
        for (int i = 0; i < values.length; i++) {
            values[i] = 0.0;
        }
        // default pretty name: class name..
        prettyName = getClass().getSimpleName();
    }

    @Override
    public String getPrettyName() {
        return prettyName;
    }
    @Override
    public void setPrettyName(String name) {
        prettyName = name;
    }

    @Override
    public void setActive(boolean active) {
        assert false : "Only telemetryProcessors can be activated";
    }

    @Override
    public double getValue(SourceDataEnum data) {
        assert provides(data) : data + " is not provided";
        synchronized(this) {
            return values[data.ordinal()];
        }
    }
    protected void setValue(SourceDataEnum data, double value) {
        assert provides(data) : data + " is not provided";
        synchronized(this) {
            values[data.ordinal()] = value;
        }
    }
    /**
     * @return -1 value is too small
     * @return 0  value is ok
     * @return 1  value is too big
     */
    @Override
    public long getModificationTime(SourceDataEnum data) {
        return 0;
    }


    @Override
    public long getLastMessageTime() {
        synchronized(this) {
            return lastMessageTime;
        }
    }

    protected long setLastMessageTime() {
        assert false : "Only sensors handles message current time!";
        return 0;
    }

    protected long setLastMessageTime(long time) {
        synchronized(this) {
            long previous = lastMessageTime;
            lastMessageTime = time;
            return previous;
        }
    }
}
