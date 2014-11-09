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
 * Test module: sets value from config in wheelSpeed
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class WheelSpeedFromConfig extends TelemetryHandler {
    private double wheelSpeed = 30.0;

    @Override
    public String getPrettyName() {
        return "robot_speed";
    }

    @Override
    public void configChanged(UserPreferences pref) {
        wheelSpeed = pref.ROBOT_SPEED.getDouble();
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return data == SourceDataEnum.WHEEL_SPEED;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        setValue(SourceDataEnum.WHEEL_SPEED, wheelSpeed);
    }
}
