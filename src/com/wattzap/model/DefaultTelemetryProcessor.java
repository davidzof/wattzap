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

import com.wattzap.controller.Messages;
import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;

/**
 *
 * @author Jarek
 */
public class DefaultTelemetryProcessor extends TelemetryProcessor {

    @Override
    public String getPrettyName() {
        return "";
    }

    boolean simulSpeed = false;
    double totalWeight = 85.0;
    RouteReader routeData = null;
    private Power power = null;
    private int resistance = 1;

    @Override
    public void configChanged(UserPreferences prefs) {
        simulSpeed = prefs.isVirtualPower();
        totalWeight = prefs.getTotalWeight();
        power = prefs.getPowerProfile();
        resistance = prefs.getResistance();
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        if (simulSpeed && (data == SourceDataEnum.WHEEL_SPEED)) {
            return true;
        }

        switch (data) {
            // main targets
            case SPEED:
            case POWER:

            // it shall be handled by TrainerHandler!
            case RESISTANCE:
            case AUTO_RESISTANCE:

            // these shall be made by RouteHandler!
            case ALTITUDE:
            case SLOPE:
            case LATITUDE:
            case LONGITUDE:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // We have a time value and rotation value, lets calculate the speed
        int powerWatts = power.getPower(t.getWheelSpeed(), t.getResistance());
        setValue(SourceDataEnum.POWER, powerWatts);

        // if we have GPX Data and Simulspeed is enabled calculate speed
        // based on power and gradient using magic sauce

        // these data shall be moved to routeHandler! Except simulated speed..
        if (routeData != null) {
            // System.out.println("gettng point at distance " + distance);
            Point p = routeData.getPoint(t.getDistance());
            if (p != null) {
                if (simulSpeed) {
                    double ratio = (powerWatts / p.getGradient());
                    // speed is video speed * power ratio
                    setValue(SourceDataEnum.WHEEL_SPEED, p.getSpeed() * ratio);
                }

                // if slope is known
                if (routeData.routeType() == RouteReader.SLOPE) {
                    double realSpeed = power.getRealSpeed(totalWeight,
                        p.getGradient() / 100, powerWatts);
                    setValue(SourceDataEnum.SPEED, realSpeed);
                }

                setValue(SourceDataEnum.ALTITUDE, p.getElevation());
                setValue(SourceDataEnum.SLOPE, p.getGradient());
                setValue(SourceDataEnum.LATITUDE, p.getLatitude());
                setValue(SourceDataEnum.LONGITUDE, p.getLongitude());
            }
        }

        // default resistance taken from preferences
        if (resistance == 0) {
            // Best matching (this is wheelSpeed best matches speed) shall be selected
            setValue(SourceDataEnum.RESISTANCE, 1);
            setValue(SourceDataEnum.AUTO_RESISTANCE, 1);
        } else {
            setValue(SourceDataEnum.RESISTANCE, resistance);
            setValue(SourceDataEnum.AUTO_RESISTANCE, 0);
        }
    }

    @Override
    public void callback(Messages m, Object o) {
        switch (m) {
            case GPXLOAD:
                routeData = (RouteReader) o;
                break;
        }
        super.callback(m, o);
    }

}
