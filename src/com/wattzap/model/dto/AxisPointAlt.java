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
public class AxisPointAlt extends AxisPoint {
    private final double alt;
    private final double lat;
    private final double lon;
    private final boolean latLon;

    public AxisPointAlt(double dist, double alt, double lat, double lon) {
        super(dist);
        this.alt = alt;
        this.lat = lat;
        this.lon = lon;
        latLon = true;
    }
    public AxisPointAlt(double dist, double alt) {
        super(dist);
        this.alt = alt;
        this.lat = 0.0;
        this.lon = 0.0;
        latLon = false;
    }

    public double getAltitude() {
        return alt;
    }

    public boolean hasLatLon() {
        return latLon;
    }
    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }

    @Override
    public String checkData(AxisPoint nextPoint) {
        AxisPointAlt next = (AxisPointAlt) nextPoint;
        if (hasLatLon() != next.hasLatLon()) {
            return "Point doesn't have LatLon";
        }
        return null;
    }

    @Override
    public String toString() {
        return "[AltPoint(" + getDistance() + ")" +
                " altitude=" + alt +
                " latitude=" + lat + (latLon ? "" : "*") +
                " longitud=" + lon + (latLon ? "" : "*") +
                "]";
    }
}
