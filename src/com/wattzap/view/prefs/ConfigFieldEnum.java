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

import com.wattzap.MsgBundle;
import com.wattzap.model.EnumerationIntf;
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

    /*
     * Enumeration value must be passed, it is easier than dealing with
     * reflection (but.. it is possible I change it in close future..)
     */
    public ConfigFieldEnum(ConfigPanel panel, UserPreferences property,
            String name, EnumerationIntf e) {
        this.property = property;
        this.enumeration = e;

        JLabel label = new JLabel();
        label.setText(MsgBundle.getString(name));
		panel.add(label);

        value = new JComboBox();
        rebuild();
        value.setActionCommand(property.getName());
        value.addActionListener(panel);
		panel.add(value, "span");
    }

    public void rebuild() {
        value.removeAllItems();
        for (int i = 0; i < enumeration.getValues().length; i++) {
            EnumerationIntf en = enumeration.getValues()[i];
            if (MsgBundle.containsKey(en.getKey())) {
                value.addItem(MsgBundle.getString(en.getKey()));
            } else {
                value.addItem(en.getKey());
            }
            if (!en.isValid()) {
                // TODO block this option..
            }
        }
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
        // nothing selected? It is possible when all items are removed to
        // initialize new list of values (eg. resistance levels depend on
        // turbo). In a second this will be fixed.. but what will happen if
        // previous value cannot be set?
        if (value.getSelectedIndex() < 0) {
            return;
        }
        EnumerationIntf e = enumeration.getValues()[value.getSelectedIndex()];
        if (!e.isValid()) {
            System.err.println("Value " + e + " is not valid");
        }
        setProperty(e);
    }
    public abstract void setProperty(EnumerationIntf val);
}
