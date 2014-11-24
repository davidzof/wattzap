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
 * Simple profile: constant power and/or constant speed. Module is selectable
 * in sensorPanel for wheelSpeed and for power
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class RobotProfile extends TelemetryHandler {
    private double power = 250.0;
    private double wheelSpeed = 30.0;

    @Override
    public String getPrettyName() {
        return "robot";
    }

    @Override
    public void configChanged(UserPreferences pref) {
        if ((pref == UserPreferences.INSTANCE) ||
            (pref == UserPreferences.ROBOT_POWER))
        {
            power = pref.getRobotPower();
        }
        if ((pref == UserPreferences.INSTANCE) ||
            (pref == UserPreferences.ROBOT_SPEED))
        {
            wheelSpeed = pref.ROBOT_SPEED.getDouble();
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case WHEEL_SPEED:
            case POWER:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // just report values set in the config
        setValue(SourceDataEnum.POWER, power);
        setValue(SourceDataEnum.WHEEL_SPEED, wheelSpeed);
    }
}
