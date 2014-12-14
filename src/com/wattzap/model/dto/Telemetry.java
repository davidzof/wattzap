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

import com.wattzap.model.PauseMsgEnum;
import com.wattzap.model.SourceDataEnum;
import java.io.Serializable;

/**
 * Data object containing all the data produced by handlers.
 *
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 * @author Jarek
 * @date Oct 15, 2014
 */
public class Telemetry implements Serializable {
    private final double[] values;
    private final TelemetryValidityEnum[] validity;
    private PauseMsgEnum pause;

    public Telemetry() {
        values = new double[SourceDataEnum.values().length];
        validity = new TelemetryValidityEnum[values.length];
        for (SourceDataEnum val : SourceDataEnum.values()) {
            values[val.ordinal()] = val.getDefault();
            validity[val.ordinal()] = TelemetryValidityEnum.NOT_PRESENT;
        }
		pause = null;
    }
    public Telemetry(PauseMsgEnum pause) {
        this();
        this.pause = pause;
        for (SourceDataEnum val : SourceDataEnum.values()) {
            switch (val) {
                // all fields are "visible" in ODO, but with no data
                case SPEED:
                case DISTANCE:
                case ALTITUDE:
                case SLOPE:
                case POWER:
                case HEART_RATE:
                case CADENCE:
                case TIME:
                    validity[val.ordinal()] = TelemetryValidityEnum.NOT_AVAILABLE;
                    break;
                default:
                    validity[val.ordinal()] = TelemetryValidityEnum.NOT_PRESENT;
                    break;
            }
        }
    }
    // copy constructor.. used to collect telemetries in a collection
    // (in general.. telemetry in TelemetryProvider is reused, so only one
    // reference will be kept in the collection..)
    public Telemetry(Telemetry t) {
        values = new double[t.values.length];
        validity = new TelemetryValidityEnum[t.validity.length];
        for (int i = 0; i < t.values.length; i++) {
            values[i] = t.values[i];
            validity[i] = t.validity[i];
        }
        pause = t.pause;
    }

    public TelemetryValidityEnum getValidity(SourceDataEnum en) {
        assert en != SourceDataEnum.PAUSE : "Cannot get validity of PAUSE";
        return validity[en.ordinal()];
    }
    public final void setValidity(SourceDataEnum en, TelemetryValidityEnum valid) {
        assert en != SourceDataEnum.PAUSE : "Cannot set validity of PAUSE";
        validity[en.ordinal()] = valid;
    }
    public boolean isAvailable(SourceDataEnum en) {
        switch (getValidity(en)) {
            case NOT_PRESENT:
            case NOT_AVAILABLE:
                return false;
            default:
                return true;
        }
    }

    public double getDouble(SourceDataEnum en) {
        assert en != SourceDataEnum.PAUSE : "Cannot get PAUSE";
        return values[en.ordinal()];
    }
    public void setDouble(SourceDataEnum en, double v, TelemetryValidityEnum valid) {
        assert en != SourceDataEnum.PAUSE : "Cannot set PAUSE";
        values[en.ordinal()] = v;
        validity[en.ordinal()] = valid;
    }
    public void setDouble(SourceDataEnum en, double v) {
        setDouble(en, v, TelemetryValidityEnum.OK);
    }

    public int getInt(SourceDataEnum en) {
        assert en != SourceDataEnum.PAUSE : "Cannot get PAUSE";
        return (int) values[en.ordinal()];
    }
    public void setInt(SourceDataEnum en, int v, TelemetryValidityEnum valid) {
        assert en != SourceDataEnum.PAUSE : "Cannot set PAUSE";
        values[en.ordinal()] = (double) v;
        validity[en.ordinal()] = valid;
    }
    public void setInt(SourceDataEnum en, int v) {
        setInt(en, v, TelemetryValidityEnum.OK);
    }

    public long getLong(SourceDataEnum en) {
        assert en != SourceDataEnum.PAUSE : "Cannot get PAUSE";
        return (long) values[en.ordinal()];
    }
    public void setLong(SourceDataEnum en, long v, TelemetryValidityEnum valid) {
        assert en != SourceDataEnum.PAUSE : "Cannot set PAUSE";
        values[en.ordinal()] = (double) v;
        validity[en.ordinal()] = valid;
    }
    public void setLong(SourceDataEnum en, long v) {
        setLong(en, v, TelemetryValidityEnum.OK);
    }


    public long getTime() {
        return getLong(SourceDataEnum.TIME);
	}
	public void setTime(long time) {
        setLong(SourceDataEnum.TIME, time);
	}

	public int getHeartRate() {
        return getInt(SourceDataEnum.HEART_RATE);
	}
	public void setHeartRate(int heartRate) {
		setInt(SourceDataEnum.HEART_RATE, heartRate);
	}

	public double getLatitude() {
        return getDouble(SourceDataEnum.LATITUDE);
	}
	public void setLatitude(double latitude) {
		setDouble(SourceDataEnum.LATITUDE, latitude);
	}

	public double getLongitude() {
        return getDouble(SourceDataEnum.LONGITUDE);
	}
	public void setLongitude(double longitude) {
		setDouble(SourceDataEnum.LONGITUDE, longitude);
	}

	public double getElevation() {
        return getDouble(SourceDataEnum.ALTITUDE);
	}
	public void setElevation(double elevation) {
		setDouble(SourceDataEnum.ALTITUDE, elevation);
	}

	public double getGradient() {
        return getDouble(SourceDataEnum.SLOPE);
	}
	public void setGradient(double gradient) {
		setDouble(SourceDataEnum.SLOPE, gradient);
	}

	public int getPower() {
        return getInt(SourceDataEnum.POWER);
	}
	public void setPower(int power) {
		setInt(SourceDataEnum.POWER, power);
	}

	public double getSpeed() {
        return getDouble(SourceDataEnum.SPEED);
	}
	public void setSpeed(double speed) {
		setDouble(SourceDataEnum.SPEED, speed);
	}

	public double getWheelSpeed() {
        return getDouble(SourceDataEnum.WHEEL_SPEED);
	}
	public void setWheelSpeed(double wspeed) {
		setDouble(SourceDataEnum.WHEEL_SPEED, wspeed);
	}

	public double getRouteSpeed() {
        return getDouble(SourceDataEnum.ROUTE_SPEED);
	}
    public long getRouteTime() {
        return getLong(SourceDataEnum.ROUTE_TIME);
    }

	public int getCadence() {
        return getInt(SourceDataEnum.CADENCE);
	}
	public void setCadence(int cadence) {
		setInt(SourceDataEnum.CADENCE, cadence);
	}

	public double getDistance() {
        return getDouble(SourceDataEnum.DISTANCE);
	}
	public void setDistance(double distance) {
		setDouble(SourceDataEnum.DISTANCE, distance);
	}

	public int getResistance() {
        return getInt(SourceDataEnum.RESISTANCE);
	}
	public void setResistance(int resistance) {
		setInt(SourceDataEnum.RESISTANCE, resistance);
	}

    public PauseMsgEnum getPause() {
        return pause;
    }
    public void setPause(PauseMsgEnum reason) {
		pause = reason;
    }

	@Override
	public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Telemetry [");
        String sep = "";
        for (SourceDataEnum val : SourceDataEnum.values()) {
            if (val == SourceDataEnum.PAUSE) {
                continue;
            }
            if ((val.getName() != null) && (getValidity(val) != TelemetryValidityEnum.NOT_PRESENT)) {
                String str = val.format(getDouble(val), true); // metric
                if (str != null) {
                    buf.append(sep);
                    buf.append(val.getName());
                    buf.append('=');
                    buf.append(str);
                    switch (getValidity(val)) {
                        case NOT_AVAILABLE:
                            buf.append('?');
                            break;
                        case TOO_BIG:
                            buf.append('+');
                            break;
                        case TOO_SMALL:
                            buf.append('-');
                            break;
                        case WRONG:
                            buf.append('!');
                            break;
                    }
                    sep = ", ";
                }
            }
        }
        buf.append(sep);
        buf.append("PAUSE=");
        buf.append(pause);
        buf.append("]");
        return buf.toString();
	}
}
