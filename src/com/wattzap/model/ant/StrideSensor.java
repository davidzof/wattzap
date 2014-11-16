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
public class StrideSensor extends AntSensor {
    private static final int STRIDE_CHANNEL_PERIOD = 8134; // ~4.03Hz
	private static final int STRIDE_DEVICE_TYPE = 124; // 0x7C

    @Override
    public int getSensorType() {
        return STRIDE_DEVICE_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return STRIDE_CHANNEL_PERIOD;
    }

    @Override
    public int getTransmissionType() {
        return 0x05;
    }

    private final AntCumulativeComp cadComp = new AntCumulativeComp(
            256, // t = 1/256s, in source 1/200: must be translated..
            16, 2048, // stride time, must be less than rotation per 8s (7.5rpm)
            8, 4, // no more than 6 strides per [s] (240rpm)
            8);

    @Override
    public void storeReceivedData(long time, int[] data) {
        switch (data[0]) {
            /* 0 Data Page Number {1 Byte} {N/A} {N/A}
             *   Data Page Number = 0x01
             * 1 Time - Fractional {1 Byte} {1/200 sec} {N/A}
             *   Fractional SDM sensor time of the last distance and/or speed
             *   computation. Set to 0x00 when unused
             * 2 Time - Integer {1 Byte} {Seconds (s)} {256}
             *   SDM time of the last distance and/or speed computation. Time
             *   starts when SDM is powered ON and continues until it is powered
             *   OFF. Set to 0x00 when unused.
             * 3 Distance - Integer {1 Byte} {meters (m)} {256}
             *   Accumulated distance. Set to 0x00 when unused.
             * 4 Distance - Fractional {Upper 4 bits} {1/16 meters} {N/A}
             *   Fractional distance. Set to 0x00 when unused.
             * 4 Instantaneous Speed - Integer {Lower 4 bits} {m/s} {N/A}
             *   Instantaneous speed is intended to be appropriately filtered by
             *   the SDM, such that the receiving unit can directly display this
             *   value to the user. Set to 0x00 when unused.
             * 5 Instantaneous Speed – Fractional {1 Byte} {1/256 m/s} {N/A}
             *   Fractional instantaneous speed. Set to 0x00 when unused.
             * 6 Stride Count {1 Byte} {Strides} {256}
             *   Accumulated strides. This value is incremented once for every
             *   two footfalls. This is a required field.
             * 7 Update Latency {1 Byte} {1/32 sec} {N/A}
             *   The time elapsed between the last speed and distance
             *   computation and the transmission of this message. Set to 0x00
             *   when unused.
             * QUESTIONS to Adiddas sensor or general?
             * Why sometimes sensor reports changes after 5-6s?
             * When cadence is more than about 135, some strides are lost..
             */
            case 0x01:
                double cad = cadComp.compute(time,
                        (data[2] << 8) + ((data[1] * 256) / 200),
                        data[6]);
                if (cad > 0.0) {
                    setValue(SourceDataEnum.CADENCE, 60 * cad);
                }
                break;
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case CADENCE:
                return true;
            default:
                return false;
        }
    }
}
