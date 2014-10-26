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

/**
 * Power profile which runs video with 1:1 speed, this is calculates power
 * which is value reversed for video speed.
 * @author Jarek
 */
public class SensorOnlyPowerProfile extends VirtualPowerProfile {
    @Override
    public String getPrettyName() {
        return "sensorPowerProfile";
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return (data == SourceDataEnum.PAUSE);
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        SensorIntf sensor = TelemetryProvider.INSTANCE.getSensor(SourceDataEnum.POWER);
        if (sensor == null) {
            setValue(SourceDataEnum.PAUSE, 250);
        } else {
            // sensor is available, but might not work: it is "handled" by
            // general speedPause condition
            setValue(SourceDataEnum.PAUSE, 0);
        }
    }
}
