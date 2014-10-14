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

/**
 *
 * @author Jarek
 */
public enum VirtualPowerEnum implements HandlerEnumerationIntf {
    // computes power from wheelSpeed
    SPEED_TO_POWER("speed2power", DefaultTelemetryProcessor.class),
    // computes wheel speed from power. If (any) power sensor is not
    // available, it is impossible to run in this mode.
    POWER_TO_SPEED("power2speed"),
    // simulated speed, it shows wheel speed to be followed.
    // Base on slope: at highest slope FTP is to be touched, when flat half of
    // FTP is taken into consideration
    FTP_SIMULATION("simulSpeed"),
    // compute wheel speed which is necessary to run video with 1:1 speed
    VIDEO_SPEED("videoSpeed");

    private final String key;
    private final Class clazz;

    private VirtualPowerEnum(String key) {
        this(key, null);
    }
    private VirtualPowerEnum(String key, Class clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean isValid() {
        // valid is handler exists
        return findActiveHandler() != null;
    }

    @Override
    public EnumerationIntf[] getValues() {
        return VirtualPowerEnum.values();
    }

    @Override
    public SourceDataProcessorIntf findActiveHandler() {
        if (clazz == null) {
            return null;
        }
        for (SourceDataProcessorIntf handler : TelemetryProvider.INSTANCE.getHandlers()) {
            if (handler.getClass().equals(clazz)) {
                return handler;
            }
        }
        return null;
    }
}
