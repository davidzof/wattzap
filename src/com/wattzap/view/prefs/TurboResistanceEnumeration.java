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
import com.wattzap.model.power.Power;

/**
 *
 * @author Jarek
 */
public class TurboResistanceEnumeration implements EnumerationIntf {

    private static TurboResistanceEnumeration[] levels = null;

    public static TurboResistanceEnumeration[] getLevels() {
        if (levels == null) {
            rebuild();
        }
        return levels;
    }

    public static void rebuild() {
        int num = 1;
        Power power = UserPreferences.TURBO_TRAINER.getTurboTrainerProfile();
        if (power != null) {
            num = power.getResitanceLevels();
        }
        levels = new TurboResistanceEnumeration[num + 1];
        for (int i = 0; i <= num; i++) {
            levels[i] = new TurboResistanceEnumeration(i);
        }
    }

    public static TurboResistanceEnumeration get(int level) {
        assert levels != null : "Levels not initialized";
        if ((level >= 0) && (level < levels.length)) {
            return levels[level];
        } else {
            return null;
        }
    }


    private int level;

    private TurboResistanceEnumeration(int level) {
        this.level = level;
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

    @Override
    public String toString() {
        return "Resistance[" + getKey() + "]";
    }
}
