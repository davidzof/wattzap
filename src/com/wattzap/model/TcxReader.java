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
package com.wattzap.model;

import com.wattzap.model.dto.AxisPointsList;
import java.io.File;

import org.jfree.data.xy.XYSeries;

import com.gpxcreator.gpxpanel.GPXFile;
import com.gpxcreator.gpxpanel.Track;
import com.gpxcreator.gpxpanel.Waypoint;
import com.gpxcreator.gpxpanel.WaypointGroup;
import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;
import com.wattzap.utils.TcxImporter;
import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/*
 * Wrapper class for GPX Track. Performs some analysis such as calculating
 * instantaneous speed, average gradient etc.
 *
 * @author David George (c) Copyright 2013
 * @date 11 June 2013
 */
@RouteAnnotation
public class TcxReader extends RouteReader {
    private double totalWeight = 85.0;
    private Power power = null;

	private AxisPointsList<Point> points = null;

    @Override
	public String getExtension() {
		return "tcx";
	}

    private static boolean importXml(DefaultHandler handler, File file) {
        if (!file.exists()) {
            return false;
        }

        XMLReader xmlReader;
        try {
            xmlReader = XMLReaderFactory.createXMLReader();
        } catch (SAXException ex) {
            return false;
        }
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);

        try {
            FileReader fileReader = new FileReader(file);
			xmlReader.parse(new InputSource(fileReader));
			fileReader.close();
            return true;
        } catch (IOException | SAXException ex) {
            return false;
        }
    }

    /**
	 * Load and reprocess data from file.
     * There can be video file attached, but it has sense only if video was
     * recorded when tcx was taken. "Virtual" tcx (created as records of the
     * session) has different times, and it has no sense! Tcx file should only
     * be used as "virtual opponent".
	 *
	 * @param file file to load
	 */
    @Override
	public String load(File file) {
        TcxImporter importer = new TcxImporter();
        if (!importXml(importer, file)) {
            return "Cannot import file";
        }

        points = new AxisPointsList<>();

        long startTime = -1;
        for (Telemetry data : importer.getData()) {
            if (startTime < 0) {
                startTime = data.getTime();
            }
            Point point = new Point(data, startTime);
            points.add(point);
        }

        if (points.size() < 2) {
            return "Too few points";
        }
        String ret = points.checkData();
        routeLen = importer.getDistance();
        return ret;
	}

    @Override
	public void close() {
        points = null;
        super.close();
	}

    public GPXFile createGpx() {
        // create GPX file to be shown on the map
        if (!points.get(0).hasPosition()) {
            return null;
        }
        Track track = new Track(Color.GREEN);
        track.setName(getName());
        WaypointGroup path = track.addTrackseg();
        path.setColor(Color.RED);
        path.setVisible(true);
        path.setWptsVisible(true);
        for (Point p : points) {
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

    @Override
    public XYSeries createProfile() {
        XYSeries series = new XYSeries("distance_km,altitude_m");
        for (Point point : points) {
            series.add(point.getDistance(), point.getElevation());
        }
        return series;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case PAUSE: // pause when end of route
            case SPEED: // compute speed from power

            case ROUTE_SPEED:
            case ROUTE_TIME:
            case ALTITUDE:
            case SLOPE:
                return true;

            case LATITUDE:
            case LONGITUDE:
                // there must be at least 2 points!
                return points.get(0).hasPosition();

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        Point p = points.get(t.getDistance());
        if (p != null) {
            double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                p.getGradient() / 100.0, t.getPower());
            setValue(SourceDataEnum.SPEED, realSpeed);

            // interpolate time on distance, the most important interpolation
            // other don't matter, are just for display purposes.
            // If time is not correctly interpolated, then video (speed and
            // position) are incorrectly computed and strange video effects
            // happens
            double time = p.getTime();
            Point pp = points.getNext();
            if (pp != null) {
                time = points.interpolate(t.getDistance(), time, pp.getTime());
            }
            setValue(SourceDataEnum.ROUTE_TIME, time);

            setValue(SourceDataEnum.ROUTE_SPEED, p.getSpeed());
            setValue(SourceDataEnum.ALTITUDE, p.getElevation());
            setValue(SourceDataEnum.SLOPE, p.getGradient());

            if (p.hasPosition()) {
                setValue(SourceDataEnum.LATITUDE, p.getLatitude());
                setValue(SourceDataEnum.LONGITUDE, p.getLongitude());
            }
            setPause(PauseMsgEnum.RUNNING);
        } else {
            setPause(PauseMsgEnum.END_OF_ROUTE);
            setValue(SourceDataEnum.SPEED, 0.0);
            setValue(SourceDataEnum.ROUTE_SPEED, 0.0);
        }
    }

    @Override
    public void configChanged(UserPreferences pref) {
        if ((pref == UserPreferences.INSTANCE) || (pref == UserPreferences.TURBO_TRAINER)) {
            power = pref.getTurboTrainerProfile();
        }
        // it can be updated every configChanged without checking the property..
        totalWeight = pref.getTotalWeight();
    }
}
