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

import java.io.Serializable;

/**
 * Data object for telemetry coming from the ANT Speed, Cadence and Heart Rate Sensors
 *
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class Telemetry implements Serializable {
	private double speed = -1;
	private int cadence;
	private double distance = 0.0;
	private int power = -1;
	private double elevation;
	private double gradient;
	private double latitude = 91;
	private double longitude = 181;
	private int heartRate = -1;
	private long time;
    private double wheelSpeed;
    private int resistance = 1;
    private boolean autoResistance = false;
    private boolean paused = true;

    public long getTime() {
        return time;
	}
	public void setTime(long time) {
		this.time = time;
	}

	public int getHeartRate() {
		return heartRate;
	}
	public void setHeartRate(int heartRate) {
		this.heartRate = heartRate;
	}

	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getElevation() {
		return elevation;
	}
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	public double getGradient() {
		return gradient;
	}
	public void setGradient(double gradient) {
		this.gradient = gradient;
	}

	public int getPower() {
		return power;
	}
	public void setPower(int power) {
		this.power = power;
	}

	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public int getCadence() {
		return cadence;
	}
	public void setCadence(int cadence) {
		this.cadence = cadence;
	}

	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getWheelSpeed() {
		return wheelSpeed;
	}
	public void setWheelSpeed(double v) {
		wheelSpeed = v;

	}

	public int getResistance() {
		return resistance;
	}
	public void setResistance(int v) {
		resistance =  v;
	}

    public boolean getAutoResistance() {
        return autoResistance;
    }
    public void setAutoResistance(boolean v) {
        autoResistance = v;
    }

    public boolean getPaused() {
        return paused;
    }
    public void setPaused(boolean v) {
        paused = v;
    }

	@Override
	public String toString() {
		return "Telemetry [speed=" + speed + ", cadence=" + cadence
				+ ", distance=" + distance + ", power=" + power
                + ", wheelSpeed=" + wheelSpeed + ", resistance=" + resistance
                + (autoResistance ? "*" : "") + (paused ? ", paused" : "")
				+ ", elevation=" + elevation + ", gradient=" + gradient
				+ ", latitude=" + latitude + ", longitude=" + longitude
				+ ", heartRate=" + heartRate + " tt " + heartRate + ", time=" + time / 1000 + "]";
	}
}
