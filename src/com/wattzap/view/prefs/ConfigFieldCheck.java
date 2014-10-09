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

import com.wattzap.model.UserPreferences;
import javax.swing.JCheckBox;

/**
 *
 * @author Jarek
 */
public abstract class ConfigFieldCheck implements ConfigFieldIntf {
    private final UserPreferences property;

    private final JCheckBox value;

    public ConfigFieldCheck(ConfigPanel panel, UserPreferences property, String name) {
        this.property = property;

        if (UserPreferences.INSTANCE.messages.containsKey(name)) {
    		value = new JCheckBox(UserPreferences.INSTANCE.messages.getString(name));
        } else {
            value = new JCheckBox(name);
        }
        value.setActionCommand(property.getName());
        value.addActionListener(panel);
		panel.add(value, "span");
    }

    @Override
    public String getName() {
        // must be same as during registration in value field..
        return property.getName();
    }

    @Override
    public void propertyChanged(UserPreferences prop, String changed) {
        if ((prop == property) || (prop == UserPreferences.INSTANCE)) {
            value.setSelected(getProperty());
        }
    }
    public abstract boolean getProperty();


    @Override
    public void fieldChanged() {
        setProperty(value.isSelected());
    }
    public abstract void setProperty(boolean val);
}
