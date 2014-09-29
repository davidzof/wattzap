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
public abstract class SourceDataHandlerAbstract implements SourceDataHandlerIntf {
    protected final double[] values = new double[SourceDataEnum.values().length];
    protected final long[] modifications = new long[SourceDataEnum.values().length];

    // handler doesn't provide connectivity information.
    protected long lastMessageTime = -1;

    public SourceDataHandlerAbstract() {
        // initialize all values to not modified
        for (int i = 0; i < modifications.length; i++) {
            modifications[i] = 0;
            values[i] = 0.0;
        }
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
    public void setValue(SourceDataEnum data, double value) {
        if (!provides(data)) {
            throw new UnsupportedOperationException(data + " is not provided");
        }
        long current = System.currentTimeMillis();
        synchronized(this) {
            values[data.ordinal()] = value;
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
    public long getLastMessageTime() {
        synchronized(this) {
            return lastMessageTime;
        }
    }
    public void setLastMessageTime() {
        long current = System.currentTimeMillis();
        synchronized(this) {
           lastMessageTime = current;
        }
    }
}
