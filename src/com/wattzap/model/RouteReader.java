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
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.AxisPointInterest;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.utils.FileName;
import java.io.File;

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
    private File currentFile = null;
    private XYSeries series = null;
    protected GPXFile gpxFile = null;

    protected double maxSlope = 0.0;
    protected double minSlope = 0.0;
    protected double routeLen = 0.0;

    // extension for this routeReader
    public abstract String getExtension();

    // full path where training file and video are located
	public String getPath() {
		return currentFile.getParent();
	}

    // name of video file, whole path is filePath / videoFile.
    // Some directory stuff might be extraordinary (useful for RLV/cycleops
    // trainings): these values are checked and stripped if necessary.
	public String getVideoFile() {
		return FileName.stripExtension(currentFile.getName()) + ".avi";
	}

    // name to be displayed above profile panel
	public String getName() {
        return FileName.stripExtension(currentFile.getName()).replace('_', ' ');
	}

    public GPXFile getGpxFile() {
        return gpxFile;
    }

    public XYSeries getSeries() {
        return series;
    }


    // load training file and set all data.. Don't check any config flag
    // to load file, just read file and process contained data!
    public final String load(String filename) {
        currentFile = new File(filename);
        if (!currentFile.exists()) {
            return "File doesn't exist";
        }
        AxisPointInterest.path = currentFile.getParent();
        String err = load(currentFile);
        if (err != null) {
            return err;
        }
        if (getName() == null) {
            return "Training without name";
        }
        return null;
    }

    public abstract String load(File file);

    public final void activate() {
        // store current configuration
        configChanged(UserPreferences.INSTANCE);
        series = createProfile();
        if (gpxFile == null) {
            gpxFile = createGpx();
        }
        MessageBus.INSTANCE.send(Messages.GPXLOAD, this);
    }

    protected void rebuildProfile() {
        if (series != null) {
            series = createProfile();
            MessageBus.INSTANCE.send(Messages.PROFILE, series);
        }
    }
    // used mostly when FTP/FTHR changed in TRN mode, to "update" profile
    protected void reloadTraining() {
        load(currentFile);
        rebuildProfile();
    }

    public XYSeries createProfile() {
        return null;
    }
    public GPXFile createGpx() {
        return null;
    }

    // Training was closed, another training will be used.
    public void close() {
        currentFile = null;
        gpxFile = null;
        series = null;

        routeLen = 0.0;
        maxSlope = 0.0;
        minSlope = 0.0;
    }

    // Returns length of the route in meters, or in ms (for TIME trainings)
    // Used by control panel.. it is max value for slider. It should be replaced
    // by clicking in Profile panel, for sure it would be more accurate.
    @Deprecated
    public final double getDistanceMeters() {
        return routeLen;
    }

    // used by simulSpeed power handler (only?)
    // TODO move it to config file..
    @Deprecated
    public double getMaxSlope() {
        return maxSlope;
    }

    @Deprecated
	public double getMinSlope() {
        return minSlope;
    }


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
