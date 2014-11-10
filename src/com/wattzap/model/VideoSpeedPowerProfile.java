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
@SelectableDataSourceAnnotation
public class VideoSpeedPowerProfile extends TelemetryHandler {
    private Power power = null;
    private double weight = 85.0;
    private Rolling speedRoll = new Rolling(24);

    @Override
    public String getPrettyName() {
        return "videoSpeed";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) ||
            (prefs == UserPreferences.TURBO_TRAINER))
        {
            power = prefs.getTurboTrainerProfile();
        }

        // when started.. speed might be very small, and route
        // doesn't progress "fast enough".
        if (prefs == UserPreferences.POWER_SOURCE) {
            speedRoll.clear();
            speedRoll.add(30);
        }

        if ((prefs == UserPreferences.INSTANCE) ||
            (prefs == UserPreferences.WEIGHT) ||
            (prefs == UserPreferences.BIKE_WEIGHT))
        {
            weight = prefs.getTotalWeight();
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case WHEEL_SPEED:
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
        double wheelSpeed = 0.0;
        double powerWatts = 0.0;

        if (power == null) {
            // profile is not selected?
        } else if (t.isAvailable(SourceDataEnum.ROUTE_SPEED)) {
            // for trainings with video speed. These are only video trainigs with
            // slope (or even with positions)
            pause = false;
            // smooth route speed a bit, in GPX speed is calculated on distance
            // and might be very jumpy.
            double routeSpeed;
            if (t.getRouteSpeed() >= 1.0) {
                routeSpeed = speedRoll.add(t.getRouteSpeed());
            } else {
                routeSpeed = speedRoll.getAverage();
            }
            powerWatts = Power.getPower(weight, t.getGradient() / 100.0, routeSpeed);
            wheelSpeed = power.getSpeed((int) powerWatts, t.getResistance());
        } else if (t.isAvailable(SourceDataEnum.TARGET_POWER)) {
            // in TRN mode.. current power equals target power, and speed is
            // calculated on power
            pause = false;
            powerWatts = t.getDouble(SourceDataEnum.TARGET_POWER);
            wheelSpeed = power.getSpeed((int) powerWatts, t.getResistance());
        } else {
            // route without video or free run.. Another profile must be taken.
            // In fact if power is taken from sensor, speed doesn't have to be
            // valid and training is paused without a reason..
        }

        setPause(pause ? PauseMsgEnum.NO_MOVEMENT : PauseMsgEnum.RUNNING);
        setValue(SourceDataEnum.WHEEL_SPEED, wheelSpeed);
        setValue(SourceDataEnum.POWER, powerWatts);
    }
}
