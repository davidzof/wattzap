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
    private int speedValue = 0;

    @Override
    public SourceDataHandlerIntf initialize() {
        super.initialize();

        // config changed is called before handler registration to initialize
        // all properties.. so it must be called once again to proper activate
        // this handler...
        configChanged(UserPreferences.VIRTUAL_POWER);
        return this;
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        // activate/deactivate on virtual power setting
        if ((prefs == UserPreferences.INSTANCE) ||
                (prefs == UserPreferences.VIRTUAL_POWER)) {
            setActive(prefs.getVirtualPower().findActiveHandler() == this);
        }
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
            // "general" target
            case POWER:
                return true;
            // wheel speed if computed
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
        speedValue = 0;
        SensorIntf sensor = TelemetryProvider.INSTANCE.getSensor(WS);
        if ((sensor != null) && (sensor.getModificationTime(WS) >
                System.currentTimeMillis() - 5000)) {
            if (wheelSpeed < sensor.getValue(WS) / 1.1) {
                speedValue = 1;
            } else if (wheelSpeed > 1.1 * sensor.getValue(WS)) {
                speedValue = -1;
            }
        }
    }
}
