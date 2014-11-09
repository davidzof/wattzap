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
 * Trainer load handler. When "auto" is set, it looks for best load to set.
 * There are two modes:
 *  - speed: when trainingSpeed and wheelSpeed are almost same
 *  - power: what load is necessary to gain required power (at current wheelSpeed)
 * In fact it works only with some speed/power data selectors. All combination
 * should be analyzed and described in details.
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class ResistanceProfile extends TelemetryHandler {
    private AutoResistanceCompEnum speedCond = AutoResistanceCompEnum.BEST_LOAD;
    private boolean autoResistance = false;
    private int resistance = 1;
    private Power power = null;

    @Override
    public String getPrettyName() {
        return "resistance";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) ||
            (prefs == UserPreferences.TURBO_TRAINER))
        {
            power = prefs.getTurboTrainerProfile();
        }

        if ((prefs == UserPreferences.INSTANCE) ||
            (prefs == UserPreferences.RESISTANCE) ||
            (prefs == UserPreferences.TURBO_TRAINER))
        {
            if (prefs.getResistance() == 0) {
                autoResistance = true;
                resistance = 1;
            } else {
                autoResistance = false;
                resistance = prefs.getResistance();
            }
        }

        // computation method
        if ((prefs == UserPreferences.INSTANCE) ||
            (prefs == UserPreferences.RESISTANCE_COMP))
        {
            speedCond = AutoResistanceCompEnum.get(prefs.getResistanceComp());
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        // resistance level is only provided when turbo has more than single
        // resistance level. Otherwise it is 1.
        if (data == SourceDataEnum.RESISTANCE) {
            return ((power != null) && autoResistance) ||
                    (resistance >= 1);
        }
        return false;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // if not configured properly.. don't produce resistance!
        if (!provides(SourceDataEnum.RESISTANCE)) {
            return;
        }

        // default resistance taken from preferences
        if (autoResistance) {
            double minDiff = computeDiff(t, resistance);
            for (int level = 1; level <= power.getResitanceLevels(); level++) {
                double diff = computeDiff(t, level);
                if (diff < minDiff) {
                    resistance = level;
                    minDiff = diff;
                }
            }
        }
        setValue(SourceDataEnum.RESISTANCE, resistance);
    }

    // compute difference (in speed) between wheelSpeed (computed from power)
    // and speed. Load which gives smallest difference is chosen.
    private double computeDiff(Telemetry t, int level) {
        // resistance must be replaced with other, level is not handled by
        // this trainer
        if (level > power.getResitanceLevels()) {
            return 9999.9;
        }

        switch (speedCond) {
            case SAME_SPEED:
                // if speed or power are not available, cannot check speed.
                if ((!t.isAvailable(SourceDataEnum.SPEED)) ||
                    (!t.isAvailable(SourceDataEnum.POWER)))
                {
                    return 0.0;
                }
                double speed = power.getSpeed(t.getPower(), level);
                speed -= t.getSpeed();
                if (speed < 0) {
                    speed = -speed;
                }
                return speed;

            case BEST_LOAD:
                // if wheelSpeed or power are not available, cannot check load.
                if ((!t.isAvailable(SourceDataEnum.WHEEL_SPEED)) ||
                    (!t.isAvailable(SourceDataEnum.POWER)))
                {
                    return 0.0;
                }
                int powerWatts = power.getPower(t.getWheelSpeed(), level);
                powerWatts -= t.getPower();
                if (powerWatts < 0) {
                    powerWatts = -powerWatts;
                }
                return powerWatts;

            default:
                // unknown computations
                return 0.0;
        }
    }
}
