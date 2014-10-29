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
 * Simple profile: constant power
 * @author Jarek
 */
public class RobotPowerProfile extends VirtualPowerProfile {
    private double power;

    @Override
    public String getPrettyName() {
        return "robot";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        super.configChanged(prefs);

        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.ROBOT_POWER)) {
            power = prefs.getRobotPower();
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // We have a time value and rotation value, lets calculate the speed
        setValue(SourceDataEnum.POWER, power);
        // report speed..
        computeSpeed(t);
    }
}
