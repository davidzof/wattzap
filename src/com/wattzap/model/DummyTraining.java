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

import com.wattzap.model.dto.Telemetry;
import java.io.File;

/**
 * Training without any data. Just free ride, only power/cadence/hr are
 * shown in the data chart.
 *
 * @author Jarek
 */
public class DummyTraining extends RouteReader {

    @Override
    public String getExtension() {
        return null;
    }

    @Override
    public String getName() {
        return "Free run";
    }

    @Override
    public String load(File file) {
        return null;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return (data == SourceDataEnum.ROUTE_TIME);
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        setValue(SourceDataEnum.ROUTE_TIME, t.getDistance() * 1000.0);
    }

    @Override
    public void configChanged(UserPreferences pref) {
    }
}
