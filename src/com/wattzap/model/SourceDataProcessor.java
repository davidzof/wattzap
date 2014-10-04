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
public abstract class SourceDataProcessor
    implements SourceDataProcessorIntf
{
    protected final double[] values = new double[SourceDataEnum.values().length];

    // has no valid information, cannot provide anything
    private long lastMessageTime = 0;

    public SourceDataProcessor() {
        // initialize all values to not modified
        for (int i = 0; i < values.length; i++) {
            values[i] = 0.0;
        }
    }

    @Override
    public void activate(boolean active) {
        throw new UnsupportedOperationException("Only telemetryProcessors can be activated");
    }

    @Override
    public double getValue(SourceDataEnum data) {
        if (!provides(data)) {
            throw new UnsupportedOperationException(data + " is not provided");
        }
        synchronized(this) {
            return values[data.ordinal()];
        }
    }
    protected void setValue(SourceDataEnum data, double value) {
        if (!provides(data)) {
            throw new UnsupportedOperationException(data + " is not provided");
        }
        synchronized(this) {
            values[data.ordinal()] = value;
        }
    }
    /* WARNING Not usable for telemetryProcessors, only valid for sensors */
    @Override
    public long getModificationTime(SourceDataEnum data) {
        throw new UnsupportedOperationException("Only sensors handle property modification time");
    }


    @Override
    public long getLastMessageTime() {
        synchronized(this) {
            return lastMessageTime;
        }
    }

    protected long setLastMessageTime() {
        throw new UnsupportedOperationException("Only sensors handles message current time!");
    }

    protected long setLastMessageTime(long time) {
        synchronized(this) {
            long previous = lastMessageTime;
            lastMessageTime = time;
            return previous;
        }
    }
}
