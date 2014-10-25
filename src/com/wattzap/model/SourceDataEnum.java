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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * List of values provided by sensors and telemetryHandlers.
 * Enum defines bundle name for label, default value, precision (for ints
 * and doubles values) or formatting, metric and imperial units, value
 * to be used to convert from metric to imperial..
 *
 * Enum has additional method for "default" conversion to string (used in ODO,
 * loggers, configFields, etc).
 *
 * Formats.. are internal property of this file.. and cannot be referenced
 * prior the enum.. That's why they are not symbolic, only ints
 *
 * @author Jarek
 */
public enum SourceDataEnum {
    // base information
    DISTANCE("distance", 0.0, 3, "km", "M", Constants.KMTOMILES),
    SPEED("speed", 0.0, 1, "km/h", "mph", Constants.KMTOMILES),
    TIME("stopwatch", 0.0, -1), // time
    PAUSE("pause", 0.0, 0), // not paused

    // sensors data
    WHEEL_SPEED("trainer_speed", 0.0, 1, "km/h", "mph", Constants.KMTOMILES),
    CADENCE("cadence", 0.0, 0, "rpm"),
    HEART_RATE("heartrate", 0.0, 0, "bpm"),
    POWER("power", 0.0, 0, "W"),
    // if trainer has only one level, no active resistance handler
    // is necessary (and value won't be shown in ODO panel)
    RESISTANCE("resistance", 1.0, 0),

    // available when SLOPE/GPX training
    ROUTE_TIME("routetime", 0.0, -1),
    ROUTE_SPEED("routespeed", 0.0, 1, "km/h", "mph", Constants.KMTOMILES),
    SLOPE("slope", 0.0, 1, "%"),
    ALTITUDE("altitude", 0.0, 0, "m", "ft", Constants.MTOFEET),
    LATITUDE("latitude", 91.0, -3), // latitude N/S
    LONGITUDE("longitude", 181.0, -4), // longitude W/E

    // available when POWER training. These values are used to set too_high
    // and too_low states for power, cadence, hr in POWER training handler.
    TARGET_POWER("targetpower", 0.0, 0, "%"), // of FTP
    TARGET_CADENCE("targetcadence", 0.0, 0, "rpm"),
    TARGET_HR("targethr", 0.0, 0, "bpm");

    // formating stuff
    private static final char digits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static SimpleDateFormat timeFormat = null;

    // real definition
    private final String name;
    private final int prec;
    private final double defVal;
    private final String metricUnit;
    private final String imperialUnit;
    private final double imperialConv;

    private SourceDataEnum(String name, double defVal, int prec) {
        this(name, defVal, prec, null);
    }
    private SourceDataEnum(String name, double defVal, int prec, String unit) {
        this(name, defVal, prec, unit, unit, 1.0);
    }
    private SourceDataEnum(
            String name, double defVal, int prec,
            String metricUnit, String imperialUnit, double imperialConv) {
        this.name = name;
        this.prec = prec;
        this.defVal = defVal;
        this.metricUnit = metricUnit;
        this.imperialUnit = imperialUnit;
        this.imperialConv = imperialConv;
    }

    public String getName() {
        return name;
    }
    public double getDefault() {
        return defVal;
    }
    public String getUnit(boolean metric) {
        if (metric) {
            return metricUnit;
        } else {
            return imperialUnit;
        }
    }
    public double getConv(boolean metric) {
        if (metric) {
            return 1.0;
        } else {
            return imperialConv;
        }
    }

    // common and fast conversion from value to string representation
    public String format(double value, boolean metric) {
        if (prec < 0) {
            switch (prec) {
                case -1:
                    if (timeFormat == null) {
                        timeFormat = new SimpleDateFormat("HH:mm:ss");
                        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    }
                    return timeFormat.format(new Date((long) value));
                case -2: // pauseMsg
                    String msg = TelemetryProvider.pauseMsg((int) value, true);
                    if (msg == null) {
                        msg = "-";
                    }
                    return msg;
                case -3: // latitude N/S
                case -4: // longitude E/W
                default:
                    // value cannot be formatted, don't show the label, etc
                    return null;
            }
        }

        if (!metric) {
            value /= imperialConv;
        }

        // default: int/double formatting
        StringBuilder buf = new StringBuilder(10);
        boolean negative = false;
        if (value < 0.0) {
            negative = true;
            value = -value;
        }

        int val = (int) value;
        value -= val;
        do {
            buf.insert(0, digits[val % 10]);
            val /= 10;
        } while (val != 0);
        if (negative) {
            buf.insert(0, '-');
        }

        if (prec != 0) {
            buf.append('.');
            for (int i = 0; i < prec; i++) {
                value *= 10;
                val = (int) value;
                value -= val;
                buf.append(digits[val]);
            }
        }
        return buf.toString();
    }
}
