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

import com.wattzap.model.AutoResistanceCompEnum;
import com.wattzap.model.EnumerationIntf;


import com.wattzap.model.UserPreferences;

/*
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 *
 * Configuration panel for turbo trainer. It
 * uses general concept of common "simple fields".
 * @author Jarek
 */
public class TurboPanel extends ConfigPanel {
	private final UserPreferences userPrefs = UserPreferences.INSTANCE;

	public TurboPanel() {
		super();

        add(new ConfigFieldEnum(this, UserPreferences.TURBO_TRAINER, "profile",
                TurboEnumeration.getTurbos()) {
            @Override
            public EnumerationIntf getProperty() {
                return TurboEnumeration.get(userPrefs.getTurboTrainerProfile());
            }
            @Override
            public void setProperty(EnumerationIntf val) {
                userPrefs.setTurboTrainer(val.getKey());
            }
        });

        add(new ConfigFieldEnum(this, UserPreferences.RESISTANCE, "resistance",
                TurboResistanceEnumeration.getLevels()) {
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
                if (prop == UserPreferences.TURBO_TRAINER) {
                    // powerProfile was changed: create new resistance levels..
                    TurboResistanceEnumeration.rebuild();
                    setEnums(TurboResistanceEnumeration.getLevels());
                    // and select current one
                    super.propertyChanged(UserPreferences.RESISTANCE, null);
                }
                super.propertyChanged(prop, changed);
            }
        });

        add(new ConfigFieldEnum(this, UserPreferences.RESISTANCE_COMP, "resistance_comp",
                AutoResistanceCompEnum.values()) {
            @Override
            public EnumerationIntf getProperty() {
                return AutoResistanceCompEnum.get(userPrefs.getResistanceComp());
            }
            @Override
            public void setProperty(EnumerationIntf val) {
                userPrefs.setResistanceComp(val.getKey());
            }
        });

 	}
}
