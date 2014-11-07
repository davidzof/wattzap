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
import com.wattzap.utils.Rolling;

/**
 * Power profile which runs video with 1:1 speed, this is calculates power
 * which is value reversed for video speed. For training mode, power equals
 * target power.. all the time.
 * @author Jarek
 */
public class VideoSpeedPowerProfile extends VirtualPowerProfile {
    private double weight = 85.0;
    private Rolling speedRoll = new Rolling(24);

    @Override
    public String getPrettyName() {
        return "videoSpeed";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        super.configChanged(prefs);

        // when started.. speed might be very small, and route
        // doesn't progress "fast enough".
        if (prefs == UserPreferences.VIRTUAL_POWER) {
            speedRoll.clear();
            speedRoll.add(30);
        }

        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.WEIGHT) ||
                (prefs == UserPreferences.BIKE_WEIGHT)) {
            weight = prefs.getTotalWeight();
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        if (data == SourceDataEnum.PAUSE) {
            return true;
        }
        return super.provides(data);
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        if (t.isAvailable(SourceDataEnum.SPEED)) {
            if (!t.isAvailable(SourceDataEnum.ROUTE_SPEED)) {
                setValue(SourceDataEnum.POWER, 0.0);
                setValue(SourceDataEnum.PAUSE, 2.0);
                return;
            }

            double avg;
            if (t.getRouteSpeed() >= 1.0) {
                avg = speedRoll.add(t.getRouteSpeed());
            } else {
                avg = speedRoll.getAverage();
            }

            int powerWatts = Power.getPower(weight, t.getGradient() / 100.0, avg);
            if (powerWatts < 0) {
                powerWatts = 0;
            }
            setValue(SourceDataEnum.POWER, powerWatts);
        } else {
            // in TRN mode.. current power equals target power. If no such..
            // free-run training?
            if (!t.isAvailable(SourceDataEnum.TARGET_POWER)) {
                setValue(SourceDataEnum.POWER, 0.0);
                setValue(SourceDataEnum.PAUSE, 2.0);
                return;
            }
            setValue(SourceDataEnum.POWER, t.getDouble(SourceDataEnum.TARGET_POWER));
        }

        setValue(SourceDataEnum.PAUSE, 0.0);
        computeSpeed(t);
    }
}
