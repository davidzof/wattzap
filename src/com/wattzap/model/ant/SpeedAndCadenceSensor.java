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
package com.wattzap.model.ant;

import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.UserPreferences;

/**
 *
 * @author Jarek
 */
public class SpeedAndCadenceSensor extends AntSensor {

    private static final int ANT_SPORT_SPEED_PERIOD = 8086;
	private static final int ANT_SPORT_SandC_TYPE = 121; // 0x78

    @Override
    public int getSensorType() {
        return ANT_SPORT_SandC_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return ANT_SPORT_SPEED_PERIOD;
    }

    @Override
    public int getTransmissionType() {
        return 1;
    }

    private final AntCumulativeComp speedComp = new AntCumulativeComp(
            4, 2, 4096, // max ticks between wheel rotations, min speed ~2km/h
            6, 2, 16, // max wheel rotations per second, max speed ~120km/h
            6 // about 1.5s to get the average
    );
    private final AntCumulativeComp cadenceComp = new AntCumulativeComp(
            0, 2, 4096, // max ticks between crank rotations, min cadence 15rpm
            2, 2, 5, // crank rotations per [s], max cadence 300rpm
            4 // about 1s to get the average
    );

    // wheel circumference [m], taken from configuration
    double wheelSize = 1.496;

    @Override
    public void storeReceivedData(long time, int[] data) {
        double speed = speedComp.compute(time, data);
        if (speed > 0.0) {
            // speed => convert to km/h
            setValue(SourceDataEnum.WHEEL_SPEED, 3.6 * wheelSize * speed);
        }
        double cadence = cadenceComp.compute(time, data);
        if (cadence > 0.0) {
            // cadence => convert to rotations per min
            setValue(SourceDataEnum.CADENCE, cadence * 60.0);
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case WHEEL_SPEED:
            case CADENCE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void configChanged(UserPreferences property) {
        wheelSize = property.getWheelsize() / 1000.0;
    }
}
