/* This file is part of Wattzap Community Edition.
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

/*
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class TurboPanel extends ConfigPanel {
	private final UserPreferences userPrefs = UserPreferences.INSTANCE;

	public TurboPanel() {
		super();

        add(new ConfigFieldEnum(this, UserPreferences.POWER_PROFILE, "profile",
                TurboEnumeration.ANY) {
            @Override
            public EnumerationIntf getProperty() {
                return TurboEnumeration.get(userPrefs.getPowerProfile());
            }
            @Override
            public void setProperty(EnumerationIntf val) {
                userPrefs.setPowerProfile(val.getKey());
            }
        });

        add(new ConfigFieldEnum(this, UserPreferences.RESISTANCE, "resistance",
                TurboResistanceEnumeration.ANY) {
            @Override
            public EnumerationIntf getProperty() {
                return TurboResistanceEnumeration.get(userPrefs.getResistance());
            }
            @Override
            public void setProperty(EnumerationIntf val) {
                userPrefs.setResistance(((TurboResistanceEnumeration) val).ordinal());
            }
            // set of levels available on turbo depends on the turbo.. Set must
            // be updated when new turbo is selected.
            // Auto level is special case, it is used for suggestions: when
            // wheelSpeed best matches simulationSpeed (this is which level shall
            // be selected).
            // When turbo has fit-profile sensor, this value is discarded at all.
            @Override
            public void propertyChanged(UserPreferences prop, String changed) {
                if (prop == UserPreferences.POWER_PROFILE) {
                    // powerProfile was changed: create new resistance levels..
                    TurboResistanceEnumeration.rebuild();
                    rebuild();
                    // and select current one
                    super.propertyChanged(UserPreferences.RESISTANCE, null);
                }
                super.propertyChanged(prop, changed);
            }
        });
	}
}
