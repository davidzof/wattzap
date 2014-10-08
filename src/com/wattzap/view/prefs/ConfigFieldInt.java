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
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 *
 * @author Jarek
 */
public abstract class ConfigFieldInt implements ConfigFieldIntf {
    private final UserPreferences property;

    private final JTextField value;

    public ConfigFieldInt(ConfigPanel panel, UserPreferences property, String name) {
        this.property = property;

        JLabel label = new JLabel();
        if (UserPreferences.INSTANCE.messages.containsKey(name)) {
    		label.setText(UserPreferences.INSTANCE.messages.getString(name));
        } else {
            label.setText(name);
        }
		panel.add(label);
		value = new JTextField(20);
        value.getDocument().putProperty("prop", property);
        value.getDocument().addDocumentListener(panel);
		panel.add(value, "span");
    }

    @Override
    public UserPreferences getProp() {
        return property;
    }

    @Override
    public void propertyChanged(UserPreferences prop, UserPreferences changed) {
        if ((property != changed) && (
                (prop == property) ||
                (prop == UserPreferences.INSTANCE))) {
            value.setText(String.format("%d", getProperty()));
        }
    }
    public abstract int getProperty();


    @Override
    public void fieldChanged() {
        int val = 0;
        boolean valid;
        try {
            val = Integer.parseInt(value.getText());
            valid = isValid(val);
        } catch (NumberFormatException nfe) {
            valid = false;
        }
        if (valid) {
            value.setBackground(Color.WHITE);
            setProperty(val);
        } else {
            value.setBackground(Color.RED);
        }
    }
    public boolean isValid(int val) {
        return true;
    }
    public abstract void setProperty(int val);
}
