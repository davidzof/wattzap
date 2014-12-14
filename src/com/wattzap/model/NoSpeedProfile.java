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

/**
 * Module that reports wheelSpeed unavailable. It is intended to be used in
 * conjunction with powerSensor only, otherwise training won't run.
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class NoSpeedProfile extends TelemetryHandler {

    @Override
    public String getPrettyName() {
        return "no_wheel_speed";
    }

   @Override
    public void configChanged(UserPreferences pref) {
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return data == SourceDataEnum.WHEEL_SPEED;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // negative speed value causes value is not present
        // (same as handler doesn't exist)
        setValue(SourceDataEnum.WHEEL_SPEED, -1.0);
    }
}
