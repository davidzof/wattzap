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
package com.wattzap.view.prefs;

import com.wattzap.model.EnumerationIntf;
import com.wattzap.model.UserPreferences;

/**
 *
 * @author Jarek
 */
public class TurboResistanceEnumeration implements EnumerationIntf {
    public static TurboResistanceEnumeration ANY;
    // it isn't real enum, it only fake it for the combo.

    static TurboResistanceEnumeration[] levels = null;
    static {
        rebuild();
    }

    public static void rebuild() {
        int num = UserPreferences.POWER_PROFILE.getPowerProfile().getResitanceLevels();
        levels = new TurboResistanceEnumeration[num + 1];
        for (int i = 0; i <= num; i++) {
            levels[i] = new TurboResistanceEnumeration(i);
        }
        ANY = levels[0];
    }

    public static TurboResistanceEnumeration get(int level) {
        assert levels != null : "Levels not initialized";
        assert (level >= 0) && (level < levels.length) :
                "Undefined level " + level + " requested";
        return levels[level];
    }


    private int level;

    private TurboResistanceEnumeration(int level) {
        this.level = level;
    }

    @Override
    public EnumerationIntf[] getValues() {
        return levels;
    }

    @Override
    public String getKey() {
        if (level == 0) {
            return "auto";
        }
        return Integer.toString(level);
    }

    @Override
    public int ordinal() {
        return level;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
