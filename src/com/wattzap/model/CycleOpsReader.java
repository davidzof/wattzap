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

import com.wattzap.model.dto.AxisPointVideo;
import com.wattzap.model.dto.AxisPointInterest;
import com.wattzap.model.dto.AxisPointAlt;
import com.gpxcreator.gpxpanel.GPXFile;
import com.gpxcreator.gpxpanel.Track;
import com.gpxcreator.gpxpanel.Waypoint;
import com.gpxcreator.gpxpanel.WaypointGroup;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.AxisPointSlope;
import com.wattzap.model.dto.AxisPointsList;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;
import com.wattzap.utils.Rolling;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Jarek
 */
@RouteAnnotation
public class CycleOpsReader extends RouteReader {
	private static Logger logger = LogManager.getLogger("CycleOpsReader");

    private double totalWeight = 85.0;
    private boolean metric = true;
    private Power power = null;

    private AxisPointsList<AxisPointAlt> altPoints = null;
    private AxisPointsList<AxisPointVideo> videoPoints = null;
    private AxisPointsList<AxisPointInterest> iPoints = null;
    private AxisPointsList<AxisPointSlope> slopePoints = null;

    private String videoTag = null;
    private String nameTag = null;

    @Override
    public String getExtension() {
        return "xml";
    }

    @Override
    public String getVideoFile() {
        if (videoTag != null) {
            return "video/" + videoTag;
        }
        return null;
    }

    @Override
    public String getName() {
        return nameTag;
    }




    @Override
    public void close() {
        super.close();
        nameTag = null;
        videoTag = null;

        altPoints = null;
        videoPoints = null;
        slopePoints = null;
        iPoints = null;
    }

    private String getText(XMLStreamReader xsr) throws XMLStreamException {
        StringBuilder bld = new StringBuilder();
        for (;;) {
            xsr.next();
            if (xsr.isCharacters()) {
                bld.append(xsr.getText());
            } else {
                break;
            }
        }
        return bld.toString();
    }

    @Override
    public String load(File file) {
        // parse track.xml file, put all points into local lists
        AxisPointsList<AxisPointAlt> altPoints = new AxisPointsList<>();
        AxisPointsList<AxisPointVideo> videoPoints = new AxisPointsList<>();
        AxisPointsList<AxisPointSlope> slopePoints = new AxisPointsList<>();
        AxisPointsList<AxisPointInterest> iPoints = new AxisPointsList<>();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return "File doesn't exist";
        }

        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = null;
        AxisPointInterest iPoint = null;
        boolean firstTag = true;

        try {
            xsr = xif.createXMLStreamReader(fis);
            while (xsr.hasNext()) {
                xsr.next();

                String ln;
                switch (xsr.getEventType()) {
                    case XMLStreamReader.START_ELEMENT:
                        ln = xsr.getLocalName();
                        if (firstTag) {
                            if (!ln.equals("Track")) {
                                return "Wrong file format";
                            }
                            firstTag = false;
                        }
                        if ((ln.equals("Name")) && (iPoint == null)) {
                            // TODO get whole string! Both characters and entities!
                            nameTag = getText(xsr);
                        } else if (ln.equals("Video")) {
                            videoTag = getText(xsr);
                        } else if (ln.equals("AltitudePoint")) {
                            double dist;
                            try {
                                dist = Double.parseDouble(
                                        xsr.getAttributeValue(null, "Distance"));
                            } catch (NullPointerException  npx) {
                                return "Distance missing, line " +
                                        xsr.getLocation().getLineNumber();
                            } catch (NumberFormatException nfe) {
                                return "Distance format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }

                            boolean latLon = true;
                            double lat;
                            try {
                                lat = Double.parseDouble(
                                        xsr.getAttributeValue(null, "Lat"));
                            } catch (NullPointerException  npx) {
                                latLon = false;
                                lat = 0.0;
                            } catch (NumberFormatException nfe) {
                                return "Latitude format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }

                            double lon;
                            try {
                                lon = Double.parseDouble(
                                        xsr.getAttributeValue(null, "Lng"));
                            } catch (NullPointerException  npx) {
                                latLon = false;
                                lon = 0.0;
                            } catch (NumberFormatException nfe) {
                                return "Longitude format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }

                            double alt;
                            try {
                                alt = Double.parseDouble(
                                        xsr.getAttributeValue(null, "Alt"));
                            } catch (NullPointerException  npx) {
                                return "Altitude missing, line " +
                                        xsr.getLocation().getLineNumber();
                            } catch (NumberFormatException nfe) {
                                return "Altitude format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }

                            if (latLon) {
                                altPoints.add(new AxisPointAlt(dist, alt, lat, lon));
                            } else {
                                altPoints.add(new AxisPointAlt(dist, alt));
                            }
                        } else if (ln.equals("VideoPoint")) {
                            double dist;
                            try {
                                dist = Double.parseDouble(
                                        xsr.getAttributeValue(null, "Distance"));
                            } catch (NullPointerException  npx) {
                                return "Distance missing, line " +
                                        xsr.getLocation().getLineNumber();
                            } catch (NumberFormatException nfe) {
                                return "Distance format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }

                            double time;
                            try {
                                time = Double.parseDouble(
                                        xsr.getAttributeValue(null, "VideoTime"));
                            } catch (NullPointerException  npx) {
                                return "Video time missing, line " +
                                        xsr.getLocation().getLineNumber();
                            } catch (NumberFormatException nfe) {
                                return "Video time format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }

                            videoPoints.add(new AxisPointVideo(dist, time));
                        } else if (ln.equals("InterestingPoint")) {
                            double dist;
                            try {
                                dist = Double.parseDouble(
                                        xsr.getAttributeValue(null, "Distance"));
                            } catch (NullPointerException  npx) {
                                return "Distance missing, line " +
                                        xsr.getLocation().getLineNumber();
                            } catch (NumberFormatException nfe) {
                                return "Distance format wrong, line " +
                                        xsr.getLocation().getLineNumber();
                            }
                            iPoint = new AxisPointInterest(dist);
                        } else if ((ln.equals("Name")) && (iPoint != null)) {
                            iPoint.setMessage(getText(xsr));
                        } else if ((ln.equals("Description")) && (iPoint != null)) {
                            iPoint.setDescription(getText(xsr));
                        } else if ((ln.equals("Image")) && (iPoint != null)) {
                            iPoint.setImage(getText(xsr));
                        }
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        ln = xsr.getLocalName();
                        if (ln.equals("InterestingPoint")) {
                            if ((iPoint != null) && (iPoint.isUsable())) {
                                iPoints.add(iPoint);
                            }
                            iPoint = null;
                        }
                        break;
                }
            }
            xsr.close();
        } catch (XMLStreamException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
            return "Wrong file format";
        }

        try {
            fis.close();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return "Cannot close file";
        }


        // sort all lists and compute all necessary values
        String ret;
        if ((ret = altPoints.checkData()) != null) {
            return "Incorrect altitudes: " + ret;
        }
        if ((ret = videoPoints.checkData()) != null) {
            return "Incorrect video times: " + ret;
        }
        if ((ret = iPoints.checkData()) != null) {
            return "Incorrect interest: " + ret;
        }



        if (altPoints.size() < 2) {
            return "Too few points";
        }
        if ((altPoints.get(0).getDistance() > 0.1) ||
                (altPoints.get(0).getDistance() < 0.0)) {
            return "Route doesn't start at 0";
        }
        routeLen = altPoints.get(altPoints.size() - 1).getDistance();

        // if no video points are defined.. Last point should be taken from
        // video.. but this is not available here. One must manually add
        // this point to the file..
        if (videoPoints.size() < 1) {
            System.err.println("No video points in the file");
            videoTag = null;
        }
        if ((videoTag != null) && (videoPoints.get(0).getDistance() < 0.0)) {
            System.err.print("First point before start (negative distance)");
            videoTag = null;
        }
        if ((videoTag != null) && (videoPoints.get(0).getDistance() > 0.1)) {
            AxisPointVideo first = new AxisPointVideo(0.0, 0.0);
            first.checkData(videoPoints.get(0));
            videoPoints.add(0, first);
        }
        if (videoTag != null) {
            double videoDist = videoPoints.get(videoPoints.size() - 1).getDistance();
            if (videoDist < routeLen) {
                System.err.println("Video points ends before the route" +
                        ", check if truncated " + (routeLen - videoDist));
                routeLen = videoDist;
            }
        }

        // compute slopes, to be used in the training.. and to keep slope smooth
        double down = 0.0;
        double up = 0.0;
        double delta = 20.0;
        Rolling avgSlope = new Rolling(10);
        // prefetch.. To have best slope: interpolate on segment starting before
        // and ending after the point. Some videos have a bit late positions
        // and pre should be bigger, eg. I saw 6seconds delay @36km/h, it is
        // 60m, so pre should be 260m.. But in general it looks more-or-less ok.
        double pre = (10 / 2) * delta;
        for (double dist = 0; dist < routeLen - delta; dist += delta) {
            double alt = altPoints.interpolate(dist,
                        altPoints.get(dist).getAltitude(),
                        altPoints.getNext().getAltitude());
            double altNext = altPoints.interpolate(dist + delta,
                        altPoints.get(dist + delta).getAltitude(),
                        altPoints.getNext().getAltitude());
            double slope = avgSlope.add(100.0 * (altNext - alt) / delta);
            if (dist > pre) {
                slopePoints.add(new AxisPointSlope(dist - pre, slope));
                //series.add((dist - pre) / 1000.0, slope);
                //series.add((dist - pre + delta) / 1000.0, slope);
            }
            if (dist < 0.1) {
                maxSlope = minSlope = slope;
            } else {
                if (slope < minSlope) {
                    minSlope = slope;
                }
                if (slope > maxSlope) {
                    maxSlope = slope;
                }
            }
            if (slope < 0) {
                down += delta * slope / 100.0;
            } else {
                up += delta * slope / 100.0;;
            }
        }
        logger.debug("Down=" + down + ", up=" + up +
                "; min=" + minSlope + ", max=" + maxSlope);

        // all data read properly
        this.altPoints = altPoints;
        this.videoPoints = videoPoints;
        this.slopePoints = slopePoints;
        this.iPoints = iPoints;
        return null;
    }

    @Override
    public XYSeries createProfile() {
        // profile depends on settings: metric or imperial
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
        for (AxisPointAlt point : altPoints) {
            if (point.getDistance() > routeLen) {
                double alt = altPoints.interpolate(routeLen,
                        altPoints.get(routeLen).getAltitude(),
                        // TODO NPE???
                        altPoints.getNext().getAltitude());
				series.add(routeLen / distConv, alt / altConv);
                break;
            }
            series.add(point.getDistance() / distConv,
                    point.getAltitude() / altConv);
        }
        return series;
    }

    public GPXFile createGpx() {
        // create GPX file to be shown on the map
        if (!altPoints.get(0).hasLatLon()) {
            return null;
        }
        int points = 0;
        Track track = new Track(Color.GREEN);
        track.setName(nameTag);
        WaypointGroup path = track.addTrackseg();
        path.setColor(Color.RED);
        path.setVisible(true);
        path.setWptsVisible(true);
        for (AxisPointAlt altPoint : altPoints) {
            Waypoint waypoint = new Waypoint(altPoint.getLat(), altPoint.getLon());
            waypoint.setEle(altPoint.getAltitude());
            waypoint.setTime(new Date((++points) * 1000));
            path.addWaypoint(waypoint);
        }

        GPXFile gpxFile = new GPXFile();
        gpxFile.getTracks().add(track);
        gpxFile.updateAllProperties();
        return gpxFile;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            // route speed/time is only available for video routes
            case ROUTE_SPEED:
            case ROUTE_TIME:
                return (videoTag != null);

            case LATITUDE:
            case LONGITUDE:
                return altPoints.get().hasLatLon();

            // available always.. or training is invalid
            case ALTITUDE:
            case SLOPE:

            case PAUSE: // pause when end of route
            case SPEED: // compute speed from power
                return true;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // route is not loaded, just ignore telemetry request
        if (altPoints == null) {
            return;
        }

        // distance in the file is [m], while in telemetry is [km]
        double dist = 1000.0 * t.getDistance();
        // If end of route.. Just stop the training
        if (dist >= routeLen) {
            setPause(PauseMsgEnum.END_OF_ROUTE);
            return;
        }

        AxisPointAlt altPoint = altPoints.get(dist);
        AxisPointAlt altNPoint = altPoints.getNext();

        if (videoTag != null) {
            AxisPointVideo videoPoint = videoPoints.get(dist);
            AxisPointVideo videoNPoint = videoPoints.getNext();
            setValue(SourceDataEnum.ROUTE_SPEED, videoPoint.getSpeed());
            // routeTime in telemetry is [ms], while file has [s]
            setValue(SourceDataEnum.ROUTE_TIME, 1000.0 * videoPoints.interpolate(
                    dist, videoPoint.getTime(), videoNPoint.getTime()));
        }

        if (altPoint.hasLatLon()) {
            setValue(SourceDataEnum.LATITUDE, altPoints.interpolate(dist,
                    altPoint.getLat(), altNPoint.getLat()));
            setValue(SourceDataEnum.LONGITUDE, altPoints.interpolate(dist,
                    altPoint.getLon(), altNPoint.getLon()));
        }

        double alt = altPoints.interpolate(dist,
                    altPoint.getAltitude(), altNPoint.getAltitude());
        setValue(SourceDataEnum.ALTITUDE, alt);
        double slope = slopePoints.get(dist).getSlope();
        setValue(SourceDataEnum.SLOPE, slope);

        // speed in telemetry is [km/h], while getRealSpeed returns [m/s]
        double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                slope / 100.0, t.getPower());
        setValue(SourceDataEnum.SPEED, realSpeed);
        setPause(PauseMsgEnum.RUNNING);

        if (sendingMessages) {
            AxisPointInterest iPoint = iPoints.get(dist);
            if ((iPoint != null) && (iPoints.isChanged()) && (iPoint.isUsable())) {
                MessageBus.INSTANCE.send(Messages.ROUTE_MSG, iPoint.getMessage());
            }
        }
    }

    @Override
    public void configChanged(UserPreferences pref) {
        if ((pref == UserPreferences.INSTANCE) || (pref == UserPreferences.TURBO_TRAINER)) {
            power = pref.getTurboTrainerProfile();
        }
        // it can be updated every configChanged without checking the property..
        totalWeight = pref.getTotalWeight();

        if ((pref == UserPreferences.INSTANCE) || (pref == UserPreferences.METRIC)) {
            metric = pref.isMetric();
            // rebuild Profile panel
            rebuildProfile();
        }
    }
}
