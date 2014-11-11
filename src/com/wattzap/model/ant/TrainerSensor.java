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
 * "Faked" sensor, just to test sensor handling mechanism.
 *
 * @author Jarek
 */
public class TrainerSensor extends AntSensor {
    private static final int HRM_CHANNEL_PERIOD = 8070;
	private static final int HRM_DEVICE_TYPE = 120; // 0x78

    private int bits = -1;

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
        setValue(SourceDataEnum.RESISTANCE, data[6]);
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case RESISTANCE:
                return true;
            default:
                return false;
        }
    }
}
