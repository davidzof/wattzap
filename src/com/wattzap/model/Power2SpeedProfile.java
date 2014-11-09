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
 * Class to compute wheel speed on power. One can use conjunction of this
 * handler and speed2power.. but it has no sense and power shall be always
 * 0. This situation is barely undetectable (only direct checking in one of
 * them is the solution)
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class Power2SpeedProfile extends TelemetryHandler {
    protected Power power = null;

    @Override
    public String getPrettyName() {
        return "power2speed";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.TURBO_TRAINER)) {
            power = prefs.getTurboTrainerProfile();
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        // TODO pause if no power
        if (data == SourceDataEnum.WHEEL_SPEED) {
            return true;
        }
        return false;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        if (power == null) {
            return;
        }
        double wheelSpeed = power.getSpeed(t.getPower(), t.getResistance());
        setValue(SourceDataEnum.WHEEL_SPEED, wheelSpeed);
    }
}
