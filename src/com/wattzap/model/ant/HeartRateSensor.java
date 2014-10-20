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
public class HeartRateSensor extends AntSensor {
    private static final int HRM_CHANNEL_PERIOD = 8070;
	private static final int HRM_DEVICE_TYPE = 120; // 0x78

    private int bits = -1;

    public HeartRateSensor(String name) {
        setPrettyName(name);
    }

    @Override
    public void configChanged(UserPreferences config) {
        /* nothing to be configured. SensorId is handled by AntSensor class */
    }

    @Override
    public int getSensorType() {
        return HRM_DEVICE_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return HRM_CHANNEL_PERIOD;
    }

    @Override
    public void storeReceivedData(long time, int[] data) {
        if (data[6] != bits) {
            bits = data[6];

            int rate = data[7];
            if ((40 < rate) && (rate <= 220)) {
                setValue(SourceDataEnum.HEART_RATE, rate);
            }
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case HEART_RATE:
                return true;
            default:
                return false;
        }
    }
}
