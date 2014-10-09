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

import com.wattzap.view.prefs.EnumerationIntf;

/**
 *
 * @author Jarek
 */
public enum VirtualPowerEnum implements EnumerationIntf {
    // computes power from wheelSpeed
    SPEED_TO_POWER("speed2power"),
    // computes wheel speed from power. If (any) power sensor is not
    // available, it is impossible to run in this mode.
    POWER_TO_SPEED("power2speed"),
    // simulated speed, it shows wheel speed to be followed.
    // Base on slope: at highest slope FTP is to be touched, when flat half of
    // FTP is taken into consideration
    FTP_SIMULATION("simulSpeed"),
    // compute wheel speed which is necessary to run video with 1:1 speed
    VIDEO_SPEED("videoSpeed");

    // TODO add handler classes, checking whether enabled, etc..

    private String key;
    private VirtualPowerEnum(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public EnumerationIntf[] getValues() {
        return VirtualPowerEnum.values();
    }
}
