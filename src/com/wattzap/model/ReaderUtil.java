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

import com.gpxcreator.gpxpanel.GPXFile;
import com.gpxcreator.gpxpanel.Track;
import com.gpxcreator.gpxpanel.Waypoint;
import com.gpxcreator.gpxpanel.WaypointGroup;
import com.wattzap.model.dto.AxisPointAltitudeIntf;
import com.wattzap.model.dto.AxisPointLatLonIntf;
import com.wattzap.model.dto.AxisPointSlopeIntf;
import com.wattzap.model.dto.AxisPointsList;
import java.awt.Color;
import java.util.Date;
import org.jfree.data.xy.XYSeries;

/**
 * Util with general profile/gpx processing
 *
 * @author Jarek
 */
public class ReaderUtil {
    public static GPXFile createGpx(String name,
            AxisPointsList<? extends AxisPointLatLonIntf> points) {
        // create GPX file to be shown on the map
        if (!points.get(0).hasPosition()) {
            return null;
        }
        Track track = new Track(Color.GREEN);
        track.setName(name);
        WaypointGroup path = track.addTrackseg();
        path.setColor(Color.RED);
        path.setVisible(true);
        path.setWptsVisible(true);
        for (AxisPointLatLonIntf p : points) {
            Waypoint waypoint = new Waypoint(p.getLatitude(), p.getLongitude());
            waypoint.setEle(p.getElevation());
            waypoint.setTime(new Date(p.getTime()));
            path.addWaypoint(waypoint);
        }

        GPXFile gpxFile = new GPXFile();
        gpxFile.getTracks().add(track);
        gpxFile.updateAllProperties();
        return gpxFile;
    }

    public static XYSeries createSlopeProfile(
            AxisPointsList<? extends AxisPointSlopeIntf> points,
            boolean metric, double routeLen)
    {
        double distConv = 1000.0;
        String format = "distance_km,slope_p";
        if (!metric) {
            distConv *= Constants.KMTOMILES;
            format = "distance_mi,slope_p";
        }
        // create altitude profile, just over points
        XYSeries series = new XYSeries(format);
        double segment = 0.0;
        double lastDist = -1.0;
        for (AxisPointSlopeIntf point : points) {
            if (lastDist < 0.0) {
                lastDist = point.getDistance() / distConv;
                segment = point.getSlope();
            } else if (point.getDistance() > routeLen) {
                series.add(lastDist, segment);
                series.add(routeLen / distConv, segment);
                break;
            } else {
                series.add(lastDist, segment);
                lastDist = point.getDistance() / distConv;
                series.add(lastDist, segment);
                segment = point.getSlope();
            }
        }
        return series;
    }

    public static XYSeries createAltitudeProfile(
            AxisPointsList<? extends AxisPointAltitudeIntf> points,
            boolean metric, double routeLen)
    {
        double distConv = 1000.0;
        double altConv = 1.0;
        String format = "distance_km,altitude_m";
        if (!metric) {
            distConv *= Constants.KMTOMILES;
            altConv *= Constants.MTOFEET;
            format = "distance_mi,altitude_feet";
        }
        // create altitude profile, just over points
        XYSeries series = new XYSeries(format);
        for (AxisPointAltitudeIntf point : points) {
            if (point.getDistance() > routeLen) {
                double alt = points.interpolate(routeLen,
                        points.get(routeLen).getAltitude(),
                        points.getNext().getAltitude());
                series.add(routeLen / distConv, alt / altConv);
                break;
            }
            series.add(point.getDistance() / distConv,
                    point.getAltitude()/ altConv);
        }
        return series;
    }

    public static XYSeries createPowerProfile(
            AxisPointsList<? extends AxisPointSlopeIntf> slopePoints,
            boolean metric, double routeLen)
    {
        String format = "time_min,power";
        // create power profile, just over points. Slope means power..
        XYSeries series = new XYSeries(format);
        for (int i = 0; i < slopePoints.size(); i++) {
            AxisPointSlopeIntf item = slopePoints.get(i);
            series.add(item.getDistance() / 60.0, item.getSlope());
            if (i == slopePoints.size() - 1) {
                series.add(routeLen / 60.0, item.getSlope());
            } else {
                series.add(slopePoints.get(i + 1).getDistance() / 60.0,
                        item.getSlope());
            }
        }
        return series;
    }
}
