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
 *
 * @author Jarek
 */
public abstract class ItemValue {
    // if min/max are not set, +- 5% are set for values
    private double min = -1.0;
    private double avg = -1.0;
    private double max = -1.0;
    private String descr = null;

    // any; value "-" value or value ".." value
    public boolean parseRange(String str, double val, String unit) {
        int len;
        int pos;
        if ((pos = str.indexOf("-")) > 0) {
            len = 1;
        } else if ((pos = str.indexOf("..")) > 0) {
            len = 2;
        } else {
            return false;
        }

        // handle pairs of same value, eg: rpe3-6, 20-25%, 150-200W, etc.
        // if different types of values, just try to parse it with default
        // rule.
        int i;
        int first = 0;
        for (i = 0; i < str.length(); i++) {
            if ("0123456789.-".indexOf(str.charAt(i)) >= 0) {
                first = i;
                break;
            }
        }
        int last = str.length();
        for (i = str.length(); i > 0; i--) {
            if ("0123456789.-".indexOf(str.charAt(i - 1)) >= 0) {
                last = i;
                break;
            }
        }

        if ((last != str.length()) && parse(str.substring(0, pos) + str.substring(last), val, "")) {
            // hmm. found, so it matches
        } else if (!parse(str.substring(0, pos), val, "")) {
            return false;
        }
        double _min = avg;
        String _descr = descr;
        if ((first != 0) && parse(str.substring(0, first) + str.substring(pos + len), val, unit)) {
            // hmm. found, so it matches
        } else if (!parse(str.substring(pos + len), val, unit)) {
            return false;
        }
        min = _min;
        max = avg;

        if (min > max) {
            return false;
        }
        avg = (min + max) / 2;

        if (_descr.startsWith("&lt;") && descr.startsWith("&gt;")) {
            return false;
        } else if (_descr.startsWith("&lt;")) {
            descr = "&lt;" + descr;
        } else if (descr.startsWith("&gt;")) {
            descr = "&gt;" + _descr;
        } else {
            descr = _descr + "-" + descr;
        }
        return true;
    }

    // any; "<" followed by value
    public boolean parseLess(String str, double val, String unit) {
        if (str.indexOf("<") != 0) {
            return false;
        }
        if (parse(str.substring(1).trim(), val, unit)) {
            min = -1.0;
            max = avg;
            // <z1 condition
            if (!descr.startsWith("&lt;")) {
                descr = "&lt;" + descr;
            }
            return true;
        } else {
            return false;
        }
    }

    // any; ">" followe by value
    public boolean parseMore(String str, double val, String unit) {
        if (str.indexOf(">") != 0) {
            return false;
        }
        if (parse(str.substring(1).trim(), val, unit)) {
            min = avg;
            max = -1.0;
            // >z7 condition
            if (!descr.startsWith("&gt;")) {
                descr = "&gt;" + descr;
            }
            return true;
        } else {
            return false;
        }
    }


    // General calculations
    private boolean parseInt(String str, String unit) {
        if (str.toLowerCase().endsWith(unit)) {
            try {
                avg = Integer.parseInt(str.substring(0, str.length() - unit.length()));
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        } else {
            return false;
        }
    }
    private boolean parseDouble(String str, String unit) {
        if (str.toLowerCase().endsWith(unit)) {
            try {
                avg = Double.parseDouble(str.substring(0, str.length() - unit.length()));
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        } else {
            return false;
        }
    }

    // val percent without following '%'
    public boolean parsePercentD(String str, double val, String unit) {
        if (!parseDouble(str, "")) {
            return false;
        }
        avg *= val / 100.0;
        descr = ((int) avg) + unit;
        return true;
    }
    // val percent followed by "%".
    public boolean parsePercent(String str, double val, String unit) {
        if (!parseDouble(str, "%")) {
            return false;
        }
        avg *= val / 100.0;
        descr = ((int) avg) + unit;
        return true;
    }

    /*
     * Cadence
     */

    // absolute value followed by "rpm"
    public boolean parseCadence(String str, String unit) {
        if (!parseInt(str, "rpm") && (!parseInt(str, ""))) {
            return false;
        }
        descr = ((int) avg) + unit;
        return true;
    }

    /*
     * Power
     */

    // absolute value followed by "W"
    public boolean parseWatts(String str, String unit) {
        if (!parseInt(str, "w")) {
            return false;
        }
        descr = ((int) avg) + unit;
        return true;
    }
    // power; rating of perceived exertion; min=avg, max = "next" RPE
    private static final double restPowerPerCent = 50.0;
    private static final double maxPowerPerCent = 150.0;
    public boolean parseRpe(String str, double val, String unit) {
        if (str.toLowerCase().startsWith("rpe")) {
            str = str.substring(3);
        } else {
            return false;
        }
        return parseRpeD(str, val, unit);
    }
    public boolean parseRpeD(String str, double val, String unit) {
        if (!parseDouble(str, "")) {
            return false;
        }
        double rmin = avg;
        double rmax = avg;
        if (rmax < 3.0) {
            rmax = 3.0;
        } else if (rmax < 5.0) {
            rmax += 1.0;
        } else if (rmax < 8.0) {
            rmax += 0.5;
        } else if (rmax < 10.0) {
            rmax += 0.25;
        } else if (rmax > 10.0) {
            return false;
        }
        avg = ((rmin - 3.0) / 7.0) * (maxPowerPerCent - restPowerPerCent) + restPowerPerCent;
        avg *= val / 100.0;
        if (rmin - (int) rmin > 0.1) {
            descr = ((int) avg) + unit + " (RPE" + rmin + ")";
        } else {
            descr = ((int) avg) + unit + " (RPE" + ((int) rmin) + ")";
        }
        if (rmax <= 10.0) {
            max = ((rmax - 3.0) / 7.0) * (maxPowerPerCent - restPowerPerCent) + restPowerPerCent;
            max *= val / 100.0;
        } else {
            descr = "&gt;" + descr;
        }
        return true;
    }
    // "special" value for PRE: star.
    public boolean parseRpeS(String str, double val, String unit) {
        if ((!str.equals("*")) && (!str.toLowerCase().equals("rpe*"))) {
            return false;
        }
        min = avg = maxPowerPerCent * val / 100.0;
        descr = "&gt;" + ((int) min) + unit + " (RPE*)";
        return true;
    }
    // training power zone. "zone" or "z" and int 1..7
    public boolean parsePowerZone(String str, double val, String unit) {
        if (str.toLowerCase().startsWith("zone")) {
            str = str.substring(4);
        } else if (str.toLowerCase().startsWith("z")) {
            str = str.substring(1);
        } else {
            return false;
        }
        return parsePowerZoneD(str, val, unit);
    }
    public boolean parsePowerZoneS(String str, double val, String unit) {
        str = str.trim().toLowerCase();
        if (false) {
        } else if (str.equals("ar") || str.equals("recovery") || str.equals("active recovery")) {
            str = "1";
		} else if (str.equals("endurance")) {
            str = "2";
		} else if (str.equals("tempo")) {
            str = "3";
		} else if (str.equals("lt") || str.equals("lactate") || str.equals("lactate threshold")) {
            str = "4";
		} else if (str.equals("vo2max")) {
            str = "5";
		} else if (str.equals("anaerobic") || str.equals("anaerobic capacity")) {
            str = "6";
		} else if (str.equals("neuromuscular") || str.equals("neuromuscular power")) {
            str = "7";
		} else {
            return false;
        }
        return parsePowerZoneD(str, val, unit);
    }
    // training zone as a number 1..7
    public boolean parsePowerZoneD(String str, double val, String unit) {
        str = str.trim().toLowerCase();
        if (false) {
        } else if (str.equals("1")) {
            avg = max = 0.55 * val;
            descr = "&lt;" + ((int) avg) + unit + " (Z1/Recovery)";
            return true;
        } else if (str.equals("2")) {
            descr = "Endurance";
            avg = 0.55;
            max = 0.75;
        } else if (str.equals("3")) {
            descr = "Tempo";
            avg = 0.75;
            max = 0.90;
        } else if (str.equals("4")) {
            descr = "LT";
            avg = 0.90;
            max = 1.05;
        } else if (str.equals("5") || str.toLowerCase().equals("5a")) {
            descr = "VO2Max";
            avg = 1.05;
            max = 1.20;
        } else if (str.equals("6") || str.toLowerCase().equals("5b")) {
            descr = "Anaerobic";
            avg = 1.20;
            max = 1.50;
        } else if (str.equals("7") || str.toLowerCase().equals("5c")) {
            avg = min = 1.50 * val;
            descr = "&gt;" + ((int) avg) + unit + " (Z7/Neuromuscular)";
            return true;
        } else {
            return false;
        }
        avg *= val;
        min = avg;
        max *= val;
        descr = ((int) avg) + unit + " (Z" + str + "/" + descr + ")";
        return true;
    }

    /**
     * Heart rate
     */
    public boolean parseBpm(String str, double val, String unit) {
        if (!parseInt(str, "bpm")) {
            return false;
        }
        descr = ((int) avg) + unit;
        return true;
    }

    public boolean parseHrZone(String str, double val, String unit) {
        if (str.toLowerCase().startsWith("zone")) {
            str = str.substring(4);
        } else if (str.toLowerCase().startsWith("z")) {
            str = str.substring(1);
        } else {
            return false;
        }
        return parseHrZoneD(str, val, unit);
    }
    public boolean parseHrZoneS(String str, double val, String unit) {
        str = str.trim().toLowerCase();
        if (false) {
        } else if (str.equals("ar") || str.equals("recovery") || str.equals("active recovery")) {
            str = "1";
		} else if (str.equals("endurance")) {
            str = "2";
		} else if (str.equals("tempo")) {
            str = "3";
		} else if (str.equals("lt") || str.equals("lactate") || str.equals("lactate threshold")) {
            str = "4";
		} else if (str.equals("vo2max")) {
            str = "5";
        } else {
            return false;
        }
        return parseHrZoneD(str, val, unit);
    }
    public boolean parseHrZoneD(String str, double val, String unit) {
        if (false) {
        } else if (str.equals("1")) {
            // active recovery < 68%
            avg = max = 0.68 * val;
            descr = "&lt;" + ((int) avg) + unit + " (Z1/Recovery)";
            return true;
        } else if (str.equals("2")) {
            // Endurance 69 - 83%
            descr = "Endurance";
            avg = 0.69;
            max = 0.83;
        } else if (str.equals("3")) {
            // Tempo 84 - 94%
            descr = "Tempo";
            avg = 0.84;
            max = 0.95;
        } else if (str.equals("4")) {
            // Lactate Threshold 95-105%
            descr = "LT";
            avg = 0.96;
            max = 1.05;
        } else if (str.equals("5")) {
            // VO2Max
            avg = min = 1.06 * val;
            descr = "&gt;" + ((int) avg) + unit + " (Z5/VO2Max)";
            return true;
        } else {
            return false;
        }
        avg *= val;
        min = avg;
        max *= val;
        descr = ((int) avg) + unit + " (Z" + str + "/" + descr + ")";
        return true;
    }


    // to be redefined for different columns; this is suitable for "power"
    // No default RPE conversion is in the list, it is included only for "rpe"
    // column
    abstract public boolean parseValue(String str, double val, String unit);


    public boolean parse(String str, double val, String unit) {
        min = -1.0;
        avg = -1.0;
        max = -1.0;
        descr = null;
        if (val < 1.0) {
            System.out.println("Coefficient is too small, cannot parse");
            return false;
        }
        return
                parseRange(str, val, unit) ||
                parseLess(str, val, unit) ||
                parseMore(str, val, unit) ||
                parseValue(str, val, unit) && (setMinMax(this));
    }
    private boolean setMinMax(ItemValue val) {
        if ((val.min < 0) && (val.max < 0)) {
            val.min = val.avg / 1.05;
            val.max = val.avg * 1.05;
        }
        return true;
    }



    public String getDescr() {
        return descr;
    }
    public double getMin() {
        return min;
    }
    public double getAvg() {
        return avg;
    }
    public double getMax() {
        return max;
    }
}
