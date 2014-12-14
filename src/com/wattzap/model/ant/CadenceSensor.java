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

public class CadenceSensor extends AntSensor {

    private static final int ANT_SPORT_CADENCE_PERIOD = 8102; // ~4.04Hz
	private static final int ANT_SPORT_CADENCE_TYPE = 122; // 0x7A

    @Override
    public int getSensorType() {
        return ANT_SPORT_CADENCE_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return ANT_SPORT_CADENCE_PERIOD;
    }

    @Override
    public int getTransmissionType() {
        return 1;
    }

    private final AntCumulativeComp cadenceComp = new AntCumulativeComp(
            4, 2, 4096, // max ticks between crank rotations, min cadence 15rpm
            6, 2, 5, // crank rotations per [s], max cadence 300rpm
            4 // about 1s to get the average
    );

    @Override
    public void storeReceivedData(long time, int[] data) {
        double cadence = cadenceComp.compute(time, data);
        if (cadence > 0.0) {
            // cadence => convert to rotations per min
            setValue(SourceDataEnum.CADENCE, cadence * 60.0);
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
