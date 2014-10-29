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
import com.wattzap.model.dto.TelemetryValidityEnum;

/**
 * Default profile: power bases on wheelSpeed and trainer resistance level.
 * @author Jarek
 */
public class Speed2PowerProfile extends VirtualPowerProfile {
    @Override
    public String getPrettyName() {
        return "speed2power";
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        if (data == SourceDataEnum.PAUSE) {
            return true;
        }
        return super.provides(data); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public void storeTelemetryData(Telemetry t) {
        if (t.getValidity(SourceDataEnum.WHEEL_SPEED) != TelemetryValidityEnum.NOT_PRESENT) {
            // We have a time value and rotation value, lets calculate the speed
            // if no active resistance hanlder, resistance is 1 (by default), so
            // it works fine for one level trainers.
            int powerWatts = power.getPower(t.getWheelSpeed(), t.getResistance());
            setValue(SourceDataEnum.POWER, powerWatts);
            setValue(SourceDataEnum.PAUSE, 0.0);
        } else {
            // sensor not available?? cannot run. For trainings without sensor
            // another profile must be selected..
            setValue(SourceDataEnum.PAUSE, 251.0);
            setValue(SourceDataEnum.POWER, 0.0);
        }
    }
}
