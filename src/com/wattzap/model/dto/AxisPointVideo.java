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

import com.wattzap.model.dto.AxisPoint;

/**
 *
 * @author Jarek
 */
public class AxisPointVideo extends AxisPoint {
    private final double time; // [s]
    private double speed; // [km/h]

    public AxisPointVideo(double dist, double time) {
        super(dist);
        this.time = time;
        this.speed = -1.0;
    }

    public AxisPointVideo(double dist, double time, double speed) {
        super(dist);
        this.time = time;
        this.speed = speed;
    }

    // return point time
    public double getTime() {
        return time;
    }

    // return current video speed
    public double getSpeed() {
        return speed;
    }

    @Override
    public String checkData(AxisPointIntf nextPoint) {
        AxisPointVideo next = (AxisPointVideo) nextPoint;
        if (time > next.getTime()) {
            return "Time moves back, delta " + (time - next.getTime());
        }

        if (speed < 0.0) {
            if (time < next.getTime()) {
                speed = 3.6 * (next.getDistance() - getDistance()) /
                        (next.getTime() - getTime());
                if (speed > 120.0) {
                    return "Speed bigger than 120km/h";
                }
            } else {
                speed = 30.0;
            }
        }
        return null;
    }

    @Override
    public void normalize(double ratio) {
        super.normalize(ratio);
        speed *= ratio;
    }

    @Override
    public String toString() {
        return "[VideoPoint(" + getDistance() + ")" +
                " time=" + time +
                " speed=" + speed +
                "]";
    }
}
