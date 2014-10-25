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
import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.Telemetry;
import org.jfree.data.xy.XYSeries;

/**
 * Training without any data. Just free ride, only power/cadence/hr are
 * shown in the data chart.
 *
 * @author Jarek
 */
public class DummyTraining extends RouteReader {

    @Override
    public TrainingModeEnum getMode() {
        return TrainingModeEnum.TIME;
    }

    @Override
    public String getExtension() {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getVideoFile() {
        return null;
    }

    @Override
    public String getName() {
        return "Free run";
    }

    @Override
    public GPXFile getGpxFile() {
        return null;
    }

    @Override
    public XYSeries getSeries() {
        return null;
    }

    @Override
    public Point getPoint(double distance) {
        return null;
    }

    @Override
    public String load(String filename) {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public double getMaxSlope() {
        return 0.0;
    }
    @Override
    public double getMinSlope() {
        return 0.0;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return false;
    }
    @Override
    public void storeTelemetryData(Telemetry t) {
    }

    @Override
    public void configChanged(UserPreferences pref) {
    }
}
