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
import com.wattzap.model.fortius.CmdFile;
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
    private boolean slopeType = false;

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

        if ((pgmf.getInfo().getWattSlopePulse() == 1) &&
                (pgmf.getInfo().getTimeDist() == 1))
        {
            slopeType = true;
        } else if ((pgmf.getInfo().getWattSlopePulse() == 0) &&
                (pgmf.getInfo().getTimeDist() == 0))
        {
            slopeType = false;
        } else {
            return "Unhandle type " + pgmf.getInfo().strWattSlopePulse() + "/" +
                    pgmf.getInfo().strTimeDist();
        }

        nameTag = pgmf.getInfo().getCourseName();
        routeLen = rlv.getCourseDist();
        if (routeLen < 1.0) {
            return "No proper course length";
        }

        double frameRate = rlv.getInfo().getFrameRate();
        if (frameRate < 1.0) {
            return "Too small frame rate " + frameRate;
        }

        System.out.println("Route length " + routeLen + ", frameRate=" + frameRate);

        int i;
        double length;

        // read all messages from cmd file
        int msg = 0;
        CmdFile cmdFile = null;
        RlvInfoBox message = rlv.getMessage(0);
        if (message != null) {
            cmdFile = new CmdFile(file.getAbsolutePath());
            for (msg = 0; (message = rlv.getMessage(msg)) != null; msg++) {
                message.setMessage(cmdFile.getMessage(msg));
                if (cmdFile.getMessage(msg) == null) {
                    System.err.println("Command " + msg + " has no message");
                }
            }
        }
        message = rlv.getMessage(msg = 0);

        length = 0.0;
        long previousFrame = 0;
        double previousDist = 0.0;
        for (i = 0; ; i++) {
            RlvFrameDistance fd = rlv.getPoint(i);
            if (fd == null) {
                break;
            }
            videoPoints.add(new AxisPointVideo(
                    length,
                    previousFrame / frameRate,
                    3.6 * fd.getDistancePerFrame() * frameRate));

            while ((message != null) &&
                    (message.getFrame() >= previousFrame) &&
                    (message.getFrame() < fd.getFrameNumber()))
            {
                iPoints.add(new AxisPointInterest(length +
                        (message.getFrame() - previousFrame) * previousDist,
                        message.getMessage()));
                message = rlv.getMessage(++msg);
            }

            if (slopeType) {
                length += previousDist * (fd.getFrameNumber() - previousFrame);
            } else {
                length = fd.getFrameNumber() / frameRate;
            }
            previousFrame = fd.getFrameNumber();
            previousDist = fd.getDistancePerFrame();
        }
        if ((length < routeLen / 1.1) || (length > routeLen * 1.1)) {
            return "Broken RLV file, ratio " + (length / routeLen);
        }

        // normalize to routeLen
        videoPoints.add(new AxisPointVideo(length, previousFrame / frameRate));
        videoPoints.normalize(routeLen);
        // dummy point at the end of the route.. just to proper normalization
        iPoints.add(new AxisPointInterest(length));
        iPoints.normalize(routeLen);


        // store all altitudes
        double down = 0.0;
        double up = 0.0;
        double altitude = pgmf.getInfo().getAltitudeStart();
        length = 0.0;
        for (i = 0; ; i++) {
            ProgramData pd = pgmf.getProgramData(i);
            if (pd == null) {
                break;
            }

            // is slope to be computed on average altitude??
            slopePoints.add(new AxisPointSlope(length, pd.getPulseSlopeWatts()));

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

            if (slopeType) {
                altPoints.add(new AxisPointAlt(length, altitude));
                if (pd.getPulseSlopeWatts() < 0) {
                    down -= (pd.getDurationDistance() * pd.getPulseSlopeWatts()) / 100.0;
                } else {
                    up += (pd.getDurationDistance() * pd.getPulseSlopeWatts()) / 100.0;
                }
                altitude += (pd.getDurationDistance() * pd.getPulseSlopeWatts()) / 100.0;
            }

            length += pd.getDurationDistance();
        }
        if ((length < routeLen / 1.1) || (length > routeLen * 1.1)) {
            return "Broken PGMF file, ratio " + (length / routeLen);
        }

        // slopePoints.. contains Power if power/time training
        slopePoints.add(new AxisPointSlope(length, 0.0));
        slopePoints.normalize(routeLen);

        if (slopeType) {
            altPoints.add(new AxisPointAlt(length, altitude));
            altPoints.normalize(routeLen);
            logger.debug("Down=" + down + ", up=" + up +
                    "; min=" + minSlope + ", max=" + maxSlope);
        } else {
            logger.debug("MinPower=" + minSlope + ", max=" + maxSlope);
        }

        System.out.println("Found " + i + " alt/slope points");

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

        if (slopePoints.size() < 2) {
            return "Too few points";
        }

        // all data read properly
        this.altPoints = altPoints;
        this.videoPoints = videoPoints;
        this.slopePoints = slopePoints;
        this.iPoints = iPoints;
        return null;
    }

    @Override
    public XYSeries createProfile() {
        if (slopeType) {
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
        } else {
            String format = "time_min,power";
            // create power profile, just over points. Slope means power..
            XYSeries series = new XYSeries(format);
            for (int i = 0; i < slopePoints.size(); i++) {
                AxisPointSlope item = slopePoints.get(i);
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

    public GPXFile createGpx() {
        return null;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            // always available
            case ROUTE_TIME:
            case PAUSE: // pause when end of route
                return true;

            // data when slope/distance type
            case ROUTE_SPEED:
            case ALTITUDE:
            case SLOPE:
            case SPEED:
                return slopeType;

            // data when power/time type
            case TARGET_POWER:
                return !slopeType;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // route is not loaded, just ignore telemetry request
        if (videoPoints == null) {
            return;
        }

        double dist;
        if (slopeType) {
            // distance in the file is [m], while in telemetry is [km]
            dist = 1000.0 * t.getDistance();
        } else {
            // time is [s], both in telemetry and in arrays
            dist = t.getDistance();
        }
        // If end of route.. Just stop the training
        if (dist >= routeLen) {
            setPause(PauseMsgEnum.END_OF_ROUTE);
            return;
        }



        if (slopeType) {
            AxisPointAlt altPoint = altPoints.get(dist);
            AxisPointAlt altNPoint = altPoints.getNext();

            double alt = altPoints.interpolate(dist,
                        altPoint.getAltitude(), altNPoint.getAltitude());
            setValue(SourceDataEnum.ALTITUDE, alt);

            // routeTime in telemetry is [ms], while file has [s]
            AxisPointVideo videoPoint = videoPoints.get(dist);
            AxisPointVideo videoNPoint = videoPoints.getNext();

            setValue(SourceDataEnum.ROUTE_TIME, 1000.0 * videoPoints.interpolate(
                    dist, videoPoint.getTime(), videoNPoint.getTime()));

            setValue(SourceDataEnum.ROUTE_SPEED, videoPoint.getSpeed());

            double slope = slopePoints.get(dist).getSlope();
            setValue(SourceDataEnum.SLOPE, slope);

            // speed in telemetry is [km/h], while getRealSpeed returns [m/s]
            // Reader reports speed.. but it could be default behaviour of any
            // other handler (and then reader produces only slope). But the only
            // difference is in config handling and producing the value
            double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                    slope / 100.0, t.getPower());
            setValue(SourceDataEnum.SPEED, realSpeed);
        } else {
            // power type training
            setValue(SourceDataEnum.ROUTE_TIME, 1000.0 * dist);

            double powerWatts = slopePoints.get(dist).getSlope();
            setValue(SourceDataEnum.TARGET_POWER, powerWatts);
        }

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
