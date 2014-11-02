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

import org.jfree.data.xy.XYSeries;

import com.gpxcreator.gpxpanel.GPXFile;
import com.wattzap.model.dto.Telemetry;

/**
 * Interface for loading route files. Should be subclassed to implement a new
 * file type.
 *
 * @author David George (c) Copyright 2013
 * @date 19 November 2013
 *
 * Route reader acts as sourceDataHandler: it provides some values with their
 * "validity". All these data are collected by TelemetryProvider and resend
 * as a telemetry.
 *
 * @author Jarek
 */
public abstract class RouteReader extends SourceDataHandler {
    // extension for this routeReader
    public abstract String getExtension();

    // load training file and set all data..
    public abstract String load(String filename);

    // full path where training file and video are located
    public abstract String getPath();

    // name of video file, whole path is filePath / videoFile.
    // Some directory stuff might be extraordinary (useful for RLV/cycleops
    // trainings): these values are checked and stripped if necessary.
    public abstract String getVideoFile();

	// get name from the GPX file
    public abstract String getName();

	// Used by map view, it is shown on the map
    public abstract GPXFile getGpxFile();

	// Used by profile view, gives time|distance/altitude|power values
	public abstract XYSeries getSeries();

    // Returns length of the route in meters, or in ms (for TIME trainings)
    // Used by control panel.. it is max value for slider. It should be replaced
    // by clicking in Profile panel, for sure it would be more accurate.
    @Deprecated
    public final double getDistanceMeters() {
        if (getSeries() == null) {
            return 1000.0 * 100; // 100%
        }
        return 1000.0 * (getSeries().getMaxX() - getSeries().getMinX());
    }

    // description of Profile chart axis
    public String getXKey() {
        return "distance";
    }
    public String getYKey() {
        return "altitude";
    }

    // Training was closed, another training will be used.
    public abstract void close();


    // used by simulSpeed power handler (only?)
    // TODO move it to config file..
    @Deprecated
    public abstract double getMaxSlope();

    @Deprecated
	public abstract double getMinSlope();


    // TelemetryHandler interface

    // set all data for telemetryProvider
    public abstract void storeTelemetryData(Telemetry t);

    // replace all "internal" settings
    public abstract void configChanged(UserPreferences pref);


    // RouteReaders don't handle initialize()/release(). Only for inheritance
    @Override
    public final SourceDataHandlerIntf initialize() {
        assert false : "Route reader cannot be initialized";
        return null;
    }

    @Override
    public final void release() {
        assert false : "Route reader cannot be uninitialized";
    }
}
