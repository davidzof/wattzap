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
package com.wattzap.model.power;

import com.wattzap.model.EnumerationIntf;
import com.wattzap.model.SelectableDataSource;
import com.wattzap.model.Sensor;
import com.wattzap.model.SourceDataHandlerIntf;

/**
 *
 * @author Jarek
 */
public class HandlerEnum implements EnumerationIntf {
    private final SourceDataHandlerIntf handler;
    private final SelectableDataSource parent;

    public HandlerEnum(SelectableDataSource parent, SourceDataHandlerIntf handler) {
        this.parent = parent;
        this.handler = handler;
    }

    @Override
    public String getKey() {
        return handler.getPrettyName();
    }

    @Override
    public int ordinal() {
        int i = 0;
        for (EnumerationIntf en : parent.getValues()) {
            if (en == this) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public boolean isValid() {
        // check if sensor is processing messages
        if (handler instanceof Sensor) {
            return handler.getLastMessageTime() != 0;
        }
        // non-sensor handlers are always valid
        return true;
    }

    public SourceDataHandlerIntf getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return "HandlerEnum[" + getKey() + "]";
    }

}
