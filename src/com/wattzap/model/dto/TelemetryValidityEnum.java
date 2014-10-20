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
package com.wattzap.model.dto;

/**
 * Enum to be used to compute field properness.
 * Field by default is OK, if no value computed by any handler NOT_SET.
 * If value is too big or too small and data is OK these values are set.
 * If there is a confilict, WRONG value is set.
 * These values shall be reported by telemetryHandlers: fit training file,
 * "simulation" profiles (when sensor data differ) and so on.
 *
 * @author Jarek
 */
public enum TelemetryValidityEnum {
    NOT_PRESENT,
    NOT_AVAILABLE,
    OK,
    TOO_SMALL,
    TOO_BIG,
    WRONG
}
