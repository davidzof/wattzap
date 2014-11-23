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
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.AxisPointSlope;
import com.wattzap.model.dto.AxisPointsList;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.fortius.PgmfFile;
import com.wattzap.model.fortius.ProgramData;
import com.wattzap.model.fortius.RlvFile;
import com.wattzap.model.fortius.RlvFrameDistance;
import com.wattzap.model.fortius.RlvInfoBox;
import com.wattzap.model.power.Power;
import com.wattzap.utils.FileName;
import java.io.File;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Jarek
 */
@RouteAnnotation
public class RlvReader extends RouteReader {
	private static Logger logger = LogManager.getLogger("RlvReader");

    private double totalWeight = 85.0;
    private boolean metric = true;
    private Power power = null;

    private AxisPointsList<AxisPointAlt> altPoints = null;
    private AxisPointsList<AxisPointVideo> videoPoints = null;
    private AxisPointsList<AxisPointInterest> iPoints = null;
    private AxisPointsList<AxisPointSlope> slopePoints = null;

    private String nameTag = null;

    @Override
    public String getExtension() {
        return "rlv";
    }

    @Override
    public String getName() {
        return nameTag;
    }




    @Override
    public void close() {
        super.close();
        nameTag = null;

        altPoints = null;
        videoPoints = null;
        slopePoints = null;
        iPoints = null;
    }

    @Override
    public String load(File file) {
        // parse track.xml file, put all points into local lists
        AxisPointsList<AxisPointAlt> altPoints = new AxisPointsList<>();
        AxisPointsList<AxisPointVideo> videoPoints = new AxisPointsList<>();
        AxisPointsList<AxisPointSlope> slopePoints = new AxisPointsList<>();
        AxisPointsList<AxisPointInterest> iPoints = new AxisPointsList<>();

        RlvFile rlv;
        PgmfFile pgmf;

        // rlv delivers frameRate and distances per frame
        try {
            rlv = new RlvFile(file.getAbsolutePath());
            pgmf = new PgmfFile(FileName.stripExtension(file.getAbsolutePath()) + ".pgmf");
        } catch (Error e) {
            return e.getMessage();
        }
        if ((pgmf.getInfo().getWattSlopePulse() != 1) ||
                (pgmf.getInfo().getTimeDist() != 1))
        {
            return "Unhandle type " + pgmf.getInfo().strWattSlopePulse() + "/" +
                    pgmf.getInfo().strTimeDist() + " only slope/dist are handled";
        }
        nameTag = pgmf.getInfo().getCourseName();
        routeLen = rlv.getCourseDist();
        if (routeLen < 1.0) {
            return "No proper course length";
        }

        int i;
        double distance;

        double frameRate = rlv.getInfo().getFrameRate();
        if (frameRate < 1.0) {
            return "Too small frame rate " + frameRate;
        }


        distance = 0.0;
        int msg = 0;
        RlvInfoBox message = rlv.getMessage(msg);
        long previousFrame = 0;
        double previousDist = 0.0;

        System.out.println("Frame rate " + frameRate + ", routeLength=" + routeLen);
        for (i = 0; ; i++) {
            RlvFrameDistance fd = rlv.getPoint(i);
            if (fd == null) {
                if (i == 0) {
                    return "Too few points";
                } else {
                    break;
                }
            }
            videoPoints.add(new AxisPointVideo(distance,
                    previousFrame / frameRate,
                    3.6 * fd.getDistancePerFrame() * frameRate));

            // multiple info per point? If constant speed, then might happend
            /*
            while ((message != null) &&
                    (message.getFrame() >= 0) &&
                    (message.getFrame() < frames + fd.getFrameNumber()))
            {
                iPoints.add(new AxisPointInterest(distance +
                        (message.getFrame() - frames) * fd.getFrameNumber(),
                        message.getMessage()));
                message = rlv.getMessage(++msg);
            }
            */

            distance += previousDist * (fd.getFrameNumber() - previousFrame);
            previousFrame = fd.getFrameNumber();
            previousDist = fd.getDistancePerFrame();
        }
        // normalize to routeLen
        videoPoints.add(new AxisPointVideo(distance, previousFrame / frameRate));
        videoPoints.normalize(routeLen);

        // dummy point at the end of the route.. just to proper normalization
        iPoints.add(new AxisPointInterest(distance));
        iPoints.normalize(routeLen);

        // store all altitudes
        double down = 0.0;
        double up = 0.0;
        double altitude = pgmf.getInfo().getAltitudeStart();
        distance = 0.0;
        for (i = 0; ; i++) {
            ProgramData pd = pgmf.getProgramData(i);
            if (pd == null) {
                if (i == 0) {
                    return "Too few points";
                } else {
                    break;
                }
            }
            altPoints.add(new AxisPointAlt(distance, altitude));
            // is slope to be computed on average altitude??
            slopePoints.add(new AxisPointSlope(distance, pd.getPulseSlopeWatts()));

            if (i == 0) {
                maxSlope = minSlope = pd.getPulseSlopeWatts();
            } else {
                if (pd.getPulseSlopeWatts() < minSlope) {
                    minSlope = pd.getPulseSlopeWatts();
                }
                if (pd.getPulseSlopeWatts() > maxSlope) {
                    maxSlope = pd.getPulseSlopeWatts();
                }
            }
            if (pd.getPulseSlopeWatts() < 0) {
                down -= (pd.getDurationDistance() * pd.getPulseSlopeWatts()) / 100.0;
            } else {
                up += (pd.getDurationDistance() * pd.getPulseSlopeWatts()) / 100.0;
            }

            distance += pd.getDurationDistance();
            altitude += (pd.getDurationDistance() * pd.getPulseSlopeWatts()) / 100.0;
        }
        altPoints.add(new AxisPointAlt(distance, altitude));
        altPoints.normalize(routeLen);
        slopePoints.add(new AxisPointSlope(distance, 0.0));
        slopePoints.normalize(routeLen);

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


        /*
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
        */

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
        return null;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case ROUTE_SPEED:
            case ROUTE_TIME:
            case ALTITUDE:
            case SLOPE:
            case SPEED:
            case PAUSE: // pause when end of route
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

        AxisPointVideo videoPoint = videoPoints.get(dist);
        AxisPointVideo videoNPoint = videoPoints.getNext();
        setValue(SourceDataEnum.ROUTE_SPEED, videoPoint.getSpeed());
        // routeTime in telemetry is [ms], while file has [s]
        setValue(SourceDataEnum.ROUTE_TIME, 1000.0 * videoPoints.interpolate(
                dist, videoPoint.getTime(), videoNPoint.getTime()));

        double alt = altPoints.interpolate(dist,
                    altPoint.getAltitude(), altNPoint.getAltitude());
        setValue(SourceDataEnum.ALTITUDE, alt);
        double slope = slopePoints.get(dist).getSlope();
        setValue(SourceDataEnum.SLOPE, slope);

        // speed in telemetry is [km/h], while getRealSpeed returns [m/s]
        // Reader reports speed.. but it could be default behaviour of any
        // other handler (and then reader produces only slope). But the only
        // difference is in config handling and producing the value
        double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                slope / 100.0, t.getPower());
        setValue(SourceDataEnum.SPEED, realSpeed);
        setPause(PauseMsgEnum.RUNNING);

        AxisPointInterest iPoint = iPoints.get(dist);
        if ((iPoint != null) && (iPoints.isChanged()) && (iPoint.isUsable())) {
            MessageBus.INSTANCE.send(Messages.ROUTE_MSG, iPoint.getMessage());
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
