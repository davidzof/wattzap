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
import javax.swing.JComboBox;
import javax.swing.JLabel;

/**
 *
 * @author Jarek
 */
public abstract class ConfigFieldEnum implements ConfigFieldIntf {
    private final EnumerationIntf enumeration;
    private final UserPreferences property;

    private final JComboBox value;

    public ConfigFieldEnum(ConfigPanel panel, UserPreferences property, String name, EnumerationIntf e) {
        this.property = property;
        this.enumeration = e;

        JLabel label = new JLabel();
        if (UserPreferences.INSTANCE.messages.containsKey(name)) {
    		label.setText(UserPreferences.INSTANCE.messages.getString(name));
        } else {
            label.setText(name);
        }
		panel.add(label);

        value = new JComboBox();
        for (int i = 0; i < enumeration.getValues().length; i++) {
            EnumerationIntf en = enumeration.getValues()[i];
            if (UserPreferences.INSTANCE.messages.containsKey(en.getKey())) {
                value.addItem(UserPreferences.INSTANCE.messages.getString(en.getKey()));
            } else {
                value.addItem(en.getKey());
            }
            if (!en.isValid()) {
                // TODO block this option..
            }
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
            value.setSelectedIndex(getProperty().ordinal());
        }
    }
    public abstract EnumerationIntf getProperty();


    @Override
    public void fieldChanged() {
        setProperty(enumeration.getValues()[value.getSelectedIndex()]);
    }
    public abstract void setProperty(EnumerationIntf val);
}
