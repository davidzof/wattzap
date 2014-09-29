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

import com.wattzap.controller.Messages;

/**
 *
 * @author Jarek
 */
class SourceDataHandlerDummy extends SourceDataHandlerAbstract {

    public SourceDataHandlerDummy() {
        super();
        values[SourceDataEnum.LATITUDE.ordinal()] = 181.0;
        values[SourceDataEnum.LONGITUDE.ordinal()] = 91.0;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return true;
    }

    @Override
    public void setValue(SourceDataEnum data, double value) {
        throw new UnsupportedOperationException(data + " not settable");
    }

    @Override
    public void callback(Messages m, Object o) {
        // doesn't register anything
    }
 }
