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
public class SpeedSensor extends AntSensor {

    private static final int ANT_SPORT_SPEED_PERIOD = 8118; // ~4.04Hz
	private static final int ANT_SPORT_SPEED_TYPE = 123; // 0x7B

    @Override
    public int getSensorType() {
        return ANT_SPORT_SPEED_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return ANT_SPORT_SPEED_PERIOD;
    }

    @Override
    public int getTransmissionType() {
        return 0x01;
    }

    private final AntCumulativeComp speedComp = new AntCumulativeComp(
            4, 2, 4096, // max ticks between wheel rotations, min speed ~2km/h
            6, 2, 16, // max wheel rotations per second, max speed ~120km/h
            6 // about 1.5s to get the average
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
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case WHEEL_SPEED:
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
