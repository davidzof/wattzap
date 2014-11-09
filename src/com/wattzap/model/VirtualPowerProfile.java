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
import com.wattzap.model.power.Power;

/**
 * Class to handle power profile
 * @author Jarek
 */
public abstract class VirtualPowerProfile extends TelemetryHandler {
    protected Power power = null;
    private boolean speedComputed = false;

    // whether trainerSpeed is too big/too small
    private int speedValue = 0;

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.TURBO_TRAINER)) {
            power = prefs.getTurboTrainerProfile();
        }
    }

    @Override
    public boolean checks(SourceDataEnum data) {
        return (data == SourceDataEnum.WHEEL_SPEED) && speedComputed;
    }

    @Override
    public long getModificationTime(SourceDataEnum data) {
        if (data == SourceDataEnum.WHEEL_SPEED) {
            return (long) speedValue;
        }
        return 0;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            // "general" targets
            case PAUSE:
            case POWER:
                return true;

            // report wheel speed for profiles without input data
            case WHEEL_SPEED:
                return speedComputed;

            default:
                return false;
        }
    }

    private static final SourceDataEnum WS = SourceDataEnum.WHEEL_SPEED;
    protected final void computeSpeed(Telemetry t) {
        if ((t == null) || (!t.isAvailable(SourceDataEnum.POWER))) {
            System.err.println("Power not available");
            speedComputed = false;
            return;
        }

        speedComputed = true;
        double wheelSpeed = power.getSpeed(t.getPower(), t.getResistance());
        setValue(WS, wheelSpeed);

        // Check whether real wheel speed is not too small or too big. It needs
        // working speed sensor, otherwise is not reported.
        // If any speed sensor is running, value from it is taken and checked
        // against "computed". Non-sensor value is not taken into consideration
        // (modification time is -1..1 then)
        speedValue = 0;
        SourceDataHandlerIntf selected = TelemetryProvider.INSTANCE.getSelected(WS);
        if ((selected != null) &&
            (selected.getModificationTime(WS) > System.currentTimeMillis() - 5000))
        {
            if (wheelSpeed < selected.getValue(WS) / 1.1) {
                speedValue = 1;
            } else if (wheelSpeed > 1.1 * selected.getValue(WS)) {
                speedValue = -1;
            }
        }
    }
}
