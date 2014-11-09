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

import java.util.HashMap;
import java.util.Map;

/**
 * Modes for selecting resistance level of the turbo trainer.
 * @author Jarek
 */
public enum AutoResistanceCompEnum implements EnumerationIntf {
    BEST_LOAD("best_load"),
    SAME_SPEED("same_speed");

    private final String key;

    private AutoResistanceCompEnum(String key) {
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

    private static final Map<String, AutoResistanceCompEnum> byKey;
    static {
        byKey = new HashMap<>();
        for (AutoResistanceCompEnum en : values()) {
            byKey.put(en.getKey(), en);
        }
    }

    public static AutoResistanceCompEnum get(String key) {
        return byKey.get(key);
    }
}
