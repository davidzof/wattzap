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
import com.wattzap.model.power.Power;
import com.wattzap.model.power.PowerProfiles;
import java.util.List;

/**
 *
 * @author Jarek
 */
public class TurboEnumeration implements EnumerationIntf {

    private static TurboEnumeration[] turbos = null;
    public static TurboEnumeration[] getTurbos() {
        if (turbos == null) {
            List<Power> profiles = PowerProfiles.INSTANCE.getProfiles();
            turbos = new TurboEnumeration[profiles.size()];
            for (int i = 0; i < profiles.size(); i++) {
                turbos[i] = new TurboEnumeration(profiles.get(i).description());
            }
        }
        return turbos;
    }

    public static TurboEnumeration get(Power power) {
        assert turbos != null : "Turbos not initialized";
        if (power == null) {
            System.err.println("Turbo for null power requested");
            return null;
        }
        for (int i = 0; i < turbos.length; i++) {
            if (turbos[i].getKey().equals(power.description())) {
                return turbos[i];
            }
        }
        System.err.println("Turbo " + power.description() + " not added?");
        return null;
    }

    private String profile;

    private TurboEnumeration(String profile) {
        this.profile = profile;
    }

    @Override
    public String getKey() {
        return profile;
    }

    @Override
    public int ordinal() {
        for (int i = 0; i < turbos.length; i++) {
            if (turbos[i] == this) {
                return i;
            }
        }
        assert false : "Undefined power profile " + getKey();
        return -1;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return "Trainer[" + getKey() + "]";
    }
}
