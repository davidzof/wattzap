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
 * Default profile: power bases on wheelSpeed and trainer resistance level.
 * @author Jarek
 */
public class Speed2PowerProfile extends VirtualPowerProfile {
    private Power power = null;

    @Override
    public String getPrettyName() {
        return "speed2power";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        super.configChanged(prefs);

        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.TURBO_TRAINER)) {
            power = prefs.getTurboTrainerProfile();
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // We have a time value and rotation value, lets calculate the speed
        // if no active resistance hanlder, resistance is 1 (by default), so
        // it works fine for one level trainers.
        int powerWatts = power.getPower(t.getWheelSpeed(), t.getResistance());
        setValue(SourceDataEnum.POWER, powerWatts);
    }
}
