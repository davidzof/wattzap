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
 * which is value reversed for video speed.
 * @author Jarek
 */
public class VideoSpeedPowerProfile extends VirtualPowerProfile {
    private Power power = null;
    private double weight = 85.0;
    private Rolling speedRoll = new Rolling(24);

    @Override
    public String getPrettyName() {
        return "videoSpeed";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        super.configChanged(prefs);

        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.TURBO_TRAINER)) {
            power = prefs.getTurboTrainerProfile();
        }
        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.WEIGHT) ||
                (prefs == UserPreferences.BIKE_WEIGHT)) {
            weight = prefs.getTotalWeight();
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        if (!t.isAvailable(SourceDataEnum.ROUTE_SPEED)) {
            setValue(SourceDataEnum.POWER, 0.0);
            return;
        }

        double avg;
        if (t.getRouteSpeed() >= 1.0) {
            avg = speedRoll.add(t.getRouteSpeed());
        } else {
            avg = speedRoll.getAverage();
        }
        if (avg < 6.0) {
            avg = 6.0;
        }
        int powerWatts = power.getPower(weight, t.getGradient() / 100.0, avg);
        if (powerWatts < 0) {
            powerWatts = 0;
        }
        setValue(SourceDataEnum.POWER, powerWatts);
    }
}
