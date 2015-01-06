/* This file is part of Wattzap Community Edition.
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
 * Represents a training item. Basically an object with Heart Rate, Power or Cadence targets.
 *
 * @author David George (c) Copyright 2013
 * @date 19 August 2013
 */
public class TrainingItem extends AxisPoint {

    // build functions with levels: expression parser (x, >x, <x, x-y), general
    // parser (find proper method to parse value) and value parsers (parse only
    // single value to check if valid).
    // first level return exact(x), more_than(x), less_than(x), range(x, y).
    // second level is a list of functions to parse value
    // third level are functions to parse value, either with prefix/postfix or
    // plain without value (eg. "7" is training level)

    private static int maxHr = 200;
    private static int ftp = 250;

    public static void setMaxHr(int hr) {
        maxHr = hr;
    }
    public static void setFtp(int power) {
        ftp = power;
    }

    private String description = null;
    private int interval = -1;
	private ItemValue hrItem = null;
    private ItemValue powerItem = null;
    private ItemValue cadItem = null;

    public TrainingItem(double time) {
        super(time);
    }

    @Override
	public String toString() {
		return "[time=" + getDistance() +
                getPowerMsg() + getHRMsg() + getCadenceMsg() + getIntervalMsg() +
                "]:: " + description;
	}


	public String getDescription() {
        StringBuilder b = new StringBuilder();
        if (description != null) {
            b.append("<html><center>");
            b.append(description);
        }

        String p = getPowerMsg();
        String h = getHRMsg();
        String c = getCadenceMsg();
        String i = getIntervalMsg();

        if ((!p.isEmpty()) || (!h.isEmpty()) || (!c.isEmpty())) {
            if (description == null) {
                b.append("<html><center>");
            } else {
                b.append("<br /><br />");
            }
            b.append("<font size=\"4\" color=\"darkgray\">");
            boolean f = true;
            if (!p.isEmpty()) {
                if (f) {
                    f = false;
                } else {
                    b.append(", ");
                }
                b.append(p);
            }
            if (!h.isEmpty()) {
                if (f) {
                    f = false;
                } else {
                    b.append(", ");
                }
                b.append(h);
            }
            if (!c.isEmpty()) {
                if (f) {
                    f = false;
                } else {
                    b.append(", ");
                }
                b.append(c);
            }
            if (!f) {
                b.append(", ");
            }
            b.append(i);
            b.append("</font>");
        }
        if (b.length() != 0) {
            b.append("</center></html>");
        }
        return b.toString();
	}
	public void setDescription(String description) {
		this.description = description;
	}



    public String getIntervalMsg() {
        if (interval < 1) {
            return "";
        } else if (interval > 300) {
            return "[" + (interval / 60) + "min]";
        } else if (interval < 90) {
            return "[" + interval + "s]";
        } else {
            String s = "" + (interval % 60);
            if (s.length() < 2) {
                s = "0" + s;
            }
            return "[" + (interval / 60) + ":" + s + "]";
        }
    }
    @Override
    public String checkData(AxisPointIntf next) {
        interval = (int) (next.getDistance() - getDistance());
        if (interval < 1) {
            return "Segment too short";
        }
        return super.checkData(next);
    }



    public int getHr() {
        if (hrItem != null) {
            return (int) hrItem.getAvg();
        } else {
            return 0;
        }
	}
	public String getHRMsg() {
        if (hrItem != null) {
            return hrItem.getDescr();
        } else {
            return "";
        }
	}
	public int isHRInRange(int hr) {
        if (hrItem != null) {
            if ((hrItem.getMin() > 0) && (hr < hrItem.getMin())) {
                return -1;
            }
            if ((hrItem.getMax() > 0) && (hr > hrItem.getMax())) {
                return 1;
            }
        }
        return 0;
	}
    private class HrItemValue extends ItemValue {
        @Override
        public boolean parseValue(String str, double val, String unit) {
            return
                    parseBpm(str, val, unit) || // 123bpm
                    parsePercent(str, val, unit) || // 73%
                    parseHrZoneS(str, val, unit) || // endurance
                    parseHrZone(str, val, unit) || // zone3
                    parseHrZoneD(str, val, unit) || // 3
                    parsePercentD(str, val, unit); // 73
        }
    }
	public boolean setHr(String v) {
        if (hrItem != null) {
            System.err.println("HR item already exist (" + hrItem.getDescr() + "), overwrite with new one!");
        }
        hrItem = new HrItemValue();
        return hrItem.parse(v, maxHr, "bpm");
	}


    public int getPower() {
        if (powerItem != null) {
            return (int) powerItem.getAvg();
        } else {
            return 0;
        }
	}
	public String getPowerMsg() {
        if (powerItem != null) {
            return powerItem.getDescr();
        } else {
            return "";
        }
	}
	public int isPowerInRange(int power) {
        if (powerItem != null) {
            if ((powerItem.getMin() > 0) && (power < powerItem.getMin())) {
                return -1;
            }
            if ((powerItem.getMax() > 0) && (power > powerItem.getMax())) {
                return 1;
            }
        }
        return 0;
	}
    private class PowerItemValue extends ItemValue {
        @Override
        public boolean parseValue(String str, double val, String unit) {
            return
                    parsePowerZone(str, val, unit) || // zone3
                    parsePowerZoneS(str, val, unit) || // endurance
                    parseRpe(str, val, unit) || // rpe3
                    parseRpeS(str, val, unit) || // *
                    parseWatts(str, unit) || // 123W
                    parsePercent(str, val, unit) || // 99%
                    parsePowerZoneD(str, val, unit) || // 3
                    parsePercentD(str, val, unit); // 99
        }
    }
	public boolean setPower(String v) {
        if (powerItem != null) {
            System.err.println("Power item already exist (" + powerItem.getDescr() + "), overwrite with new one!");
        }
        powerItem = new PowerItemValue();
        return powerItem.parse(v, ftp, "W");
	}
    private class RpeItemValue extends ItemValue {
        @Override
        public boolean parseValue(String str, double val, String unit) {
            return
                    parsePowerZone(str, val, unit) || // zone3
                    parsePowerZoneS(str, val, unit) || // endurance
                    parseRpe(str, val, unit) || // rpe3
                    parseRpeS(str, val, unit) || // *
                    parseWatts(str, unit) || // 123W
                    parsePercent(str, val, unit) || // 99%
                    parseRpeD(str, val, unit) || // 8
                    parsePowerZoneD(str, val, unit) || // 3
                    parsePercentD(str, val, unit); // 99
        }
    }
	public boolean setRpe(String v) {
        if (powerItem != null) {
            System.err.println("Power item already exist (" + powerItem.getDescr() + "), overwrite with new one!");
        }
        powerItem = new RpeItemValue();
        return powerItem.parse(v, ftp, "W");
	}


    public int getCadence() {
        if (cadItem != null) {
            return (int) cadItem.getAvg();
        } else {
            return 0;
        }
	}
	public String getCadenceMsg() {
        if (cadItem != null) {
            return cadItem.getDescr();
        } else {
            return "";
        }
	}
	public int isCadenceInRange(int cad) {
        if (cadItem != null) {
            if ((cadItem.getMin() > 0) && (cad < cadItem.getMin())) {
                return -1;
            }
            if ((cadItem.getMax() > 0) && (cad > cadItem.getMax())) {
                return 1;
            }
        }
        return 0;
	}
    private class CadItemValue extends ItemValue {
        @Override
        public boolean parseValue(String str, double val, String unit) {
            return
                    parseCadence(str, unit);
        }
    }
	public boolean setCadence(String v) {
        if (cadItem != null) {
            System.err.println("Cadnce item already exist (" + cadItem.getDescr() + "), overwrite with new one!");
        }
        cadItem = new CadItemValue();
        return cadItem.parse(v, 1.0, "rpm");
	}



    public static int getTrainingLevel(int power) {
		// active recovery < 55%
		int level1 = (int) ((double) ftp * 0.55);
		// Endurance 56 - 75%
		int level2 = (int) ((double) ftp * 0.75);
		// Tempo 76 - 90%
		int level3 = (int) ((double) ftp * 0.9);
		// Lactate Threshold 91-105%
		int level4 = (int) ((double) ftp * 1.05);
		// VO2Max 106-120
		int level5 = (int) ((double) ftp * 1.2);
		// Anaerobic Capacity
		int level6 = (int) ((double) ftp * 1.50);
		// Neuromuscular

		if (power >= 0 && power <= level1) {
			return 1;
		} else if (power > level1 && power <= level2) {
			return 2;
		} else if (power > level2 && power <= level3) {
			return 3;
		} else if (power > level3 && power <= level4) {
			return 4;
		} else if (power > level4 && power <= level5) {
			return 5;
		} else if (power > level5 && power <= level6) {
			return 6;
		} else {
    		return 7;
        }
	}

	public static int getHRTrainingLevel(int hr) {
		// active recovery < 68%
		int level1 = (int) ((double) maxHr * 0.68);
		// Endurance 69 - 83%
		int level2 = (int) ((double) maxHr * 0.83);
		// Tempo 84 - 94%
		int level3 = (int) ((double) maxHr * 0.94);
		// Lactate Threshold 95-105%
		int level4 = (int) ((double) maxHr * 1.05);
		// VO2Max > 105%
		int level5 = (int) ((double) maxHr * 1.05);

		if (hr >= 0 && hr <= level1) {
			return 1;
		} else if (hr > level1 && hr <= level2) {
			return 2;
		} else if (hr > level2 && hr <= level3) {
			return 3;
		} else if (hr > level3 && hr <= level4) {
			return 4;
		} else {
    		return 5;
        }
	}

	/**
	 * Helper class to convert an integer power training level to its string representation
	 *
	 * @param level (1-7)
	 * @return	Level description
	 */
	public static String getTrainingName(int level) {
		switch (level) {
		case 1:
			return "Active Recovery";
		case 2:
			return "Endurance";
		case 3:
			return "Tempo";
		case 4:
			return "Lactate";
		case 5:
			return "VO2Max";
		case 6:
			return "Anaerobic";
        default:
    		return "Neuromuscular";
		}
	}
}
