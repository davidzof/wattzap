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
 * List of "hardware" values provided by sensors
 * @author Jarek
 */
public enum SourceDataEnum {
    WHEEL_SPEED(0.0),
    CADENCE(0.0),
    HEART_RATE(0.0),
    POWER(0.0),
    RESISTANCE(1.0),
    SLOPE(0.0),
    ALTITUDE(0.0),
    LATITUDE(181.0),
    LONGITUDE(91.0),
    SPEED(0.0),
    PAUSE(0.0);

    private double defVal;

    private SourceDataEnum(double defVal) {
        this.defVal = defVal;
    }

    public double getDefault() {
        return defVal;
    }
}
