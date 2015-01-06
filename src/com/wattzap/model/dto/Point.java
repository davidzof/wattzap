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

import com.wattzap.model.SourceDataEnum;

/**
 *
 * Represents a data point from a route or power file (gpx, rlv, pwr etc)
 *
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class Point extends AxisPoint
        implements AxisPointSlopeIntf, AxisPointAltitudeIntf, AxisPointLatLonIntf
{
    private boolean hasPosition = false;
	private double latitude = -91.0;
	private double longitude = -181.0;
	private double elevation = 0.0;
	private double gradient = 0.0;
	private double speed = 0.0;
	private long time = 0;

    public Point(double distanceFromStart) {
        super(distanceFromStart);
    }


    public Point(Telemetry t, long timeOff) {
        super(t.getDistance());
        if (t.isAvailable(SourceDataEnum.LATITUDE) && t.isAvailable(SourceDataEnum.LONGITUDE)) {
            latitude = t.getLatitude();
            longitude = t.getLongitude();
            hasPosition = true;
        }
        elevation = t.getElevation();
        gradient = t.getGradient();
        speed = t.getSpeed();
        time = t.getTime() - timeOff;
    }

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public double getGradient() {
		return gradient;
	}

    // alias?? Or gradient*100?
    @Override
    public double getSlope() {
        return getGradient();
    }

	public void setGradient(double gradient) {
		this.gradient = gradient;
	}

    public boolean hasPosition() {
        return hasPosition;
    }

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
        this.hasPosition = true;
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
        this.hasPosition = true;
		this.longitude = longitude;
	}

	public double getElevation() {
		return elevation;
	}

    @Override
    public double getAltitude() {
        return getElevation();
    }

	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	public double getDistanceFromStart() {
		return getDistance();
	}

	@Override
	public String toString() {
		return "Point [distanceFromStart=" + getDistance() +
                ", time=" + time + ", speed=" + speed +
				", elevation=" + elevation + ", gradient=" + gradient +
				", latitude=" + latitude + ", longitude=" + longitude + "]";
	}
}
