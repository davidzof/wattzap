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

/**
 *
 * @author Jarek
 */
public class HeartRateSensor extends AntSensor {
    private static final int HRM_CHANNEL_PERIOD = 8070; // 4.06Hz
	private static final int HRM_DEVICE_TYPE = 120; // 0x78

    @Override
    public int getSensorType() {
        return HRM_DEVICE_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return HRM_CHANNEL_PERIOD;
    }

    @Override
    public int getTransmissionType() {
        return 1;
    }

    private final AntCumulativeComp hrComp = new AntCumulativeComp(
            4, 2, 2048, // max ticks between heart beat, min HR 30bpm
            6, 1, 4, // heartbeats per [s], max heart rate 240bpm
            8 // about 2s to get the average
    );

    @Override
    public void storeReceivedData(long time, int[] data) {
        double compRate = hrComp.compute(time, data);
        if (compRate > 0.0) {
            setValue(SourceDataEnum.HEART_RATE, compRate * 60.0);
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
