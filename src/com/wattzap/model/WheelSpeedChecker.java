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
import com.wattzap.model.power.Power;

/**
 * Reports if wheelSpeed is correct (or too small/too big). It compares
 * wheelSpeed with power, current load is checked.
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class WheelSpeedChecker extends TelemetryHandler {
    private Power power = null;

    // whether trainerSpeed is too big/too small
    private int speedValue = 0;

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.TURBO_TRAINER)) {
            power = prefs.getTurboTrainerProfile();
        }
    }

    // checks wheelSpeed (versus power)
    @Override
    public boolean checks(SourceDataEnum data) {
        return data == SourceDataEnum.WHEEL_SPEED;
    }

    // doesn't provide anything
    @Override
    public boolean provides(SourceDataEnum data) {
        return false;
    }

    @Override
    public long getModificationTime(SourceDataEnum data) {
        if (data == SourceDataEnum.WHEEL_SPEED) {
            return (long) speedValue;
        }
        return 0;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        speedValue = 0;
        if ((power != null) &&
            (t.isAvailable(SourceDataEnum.WHEEL_SPEED)) &&
            (t.isAvailable(SourceDataEnum.POWER)))
        {
            double wheelSpeed = power.getSpeed(t.getPower(), t.getResistance());
            if (wheelSpeed < t.getWheelSpeed() / 1.1) {
                speedValue = 1;
            } else if (wheelSpeed > 1.1 * t.getWheelSpeed()) {
                speedValue = -1;
            }
        }
    }
}
