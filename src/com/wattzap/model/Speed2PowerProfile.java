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
 * Power computation: wheelSpeed on slope and weight gives power.
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class Speed2PowerProfile extends TelemetryHandler {
    private Power power = null;

    @Override
    public String getPrettyName() {
        return "speed2power";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) ||
            (prefs == UserPreferences.TURBO_TRAINER))
        {
            power = prefs.getTurboTrainerProfile();
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case POWER:
            case PAUSE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        boolean pause = true;
        double powerWatts = 0.0;

        if ((t.isAvailable(SourceDataEnum.WHEEL_SPEED)) && (power != null)) {
            // for trainings with video speed. These are only video trainigs with
            // slope (or even with positions)
            pause = false;
            powerWatts = power.getPower(t.getWheelSpeed(), t.getResistance());
            if (powerWatts < 0) {
                powerWatts = 0;
            }
        }

        setPause(pause ? PauseMsgEnum.NO_MOVEMENT : PauseMsgEnum.RUNNING);
        setValue(SourceDataEnum.POWER, powerWatts);
    }
}
