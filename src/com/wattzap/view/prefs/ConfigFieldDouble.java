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
import com.wattzap.model.UserPreferences;
import java.awt.Color;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 *
 * @author Jarek
 */
public class ConfigFieldDouble implements ConfigFieldIntf {
    private final UserPreferences property;
    private final String format;

    private final String metricUnit;
    private final String imperialUnit;
    private final double imperialConv;

    private final JTextField value;
    private final JLabel unit;
    private double conversion;

    public ConfigFieldDouble(ConfigPanel panel, UserPreferences property, String name, String format) {
        this(panel, property, name, format, null);
    }
    public ConfigFieldDouble(ConfigPanel panel, UserPreferences property, String name, String format,
            String metricUnit) {
        this(panel, property, name, format, metricUnit, null, 1.0);
    }
    public ConfigFieldDouble(ConfigPanel panel, UserPreferences property, String name, String format,
            String metricUnit, String imperialUnit, double imperial) {
        this.property = property;
        this.format = format;
        this.metricUnit = metricUnit;
        if (imperialUnit == null) {
            this.imperialUnit = metricUnit;
            this.imperialConv = 1.0;
        } else {
            this.imperialUnit = imperialUnit;
            this.imperialConv = imperial;
        }

        JLabel label = new JLabel();
        label.setText(MsgBundle.getString(name));
		panel.add(label);

        value = new JTextField(20);
        value.getDocument().putProperty("name", property.getName());
        value.getDocument().addDocumentListener(panel);
        if ((metricUnit != null) || (imperialUnit != null)) {
    		panel.add(value);
            unit = new JLabel();
    		panel.add(unit, "span");
        } else {
            unit = null;
    		panel.add(value, "span");
        }
    }

    @Override
    public String getName() {
        // must be same as during registration in value field..
        return property.getName();
    }

    @Override
    public void propertyChanged(UserPreferences prop, String changed) {
        if ((prop == UserPreferences.METRIC) || (prop == UserPreferences.INSTANCE)) {
            conversion =  prop.isMetric() ? 1.0 : imperialConv;
            if (unit != null) {
                String label = prop.isMetric() ? metricUnit : imperialUnit;
                unit.setText(label);
            }
        }
        if ((!getName().equals(changed)) && (
                (prop == property) ||
                (prop == UserPreferences.INSTANCE) ||
                (prop == UserPreferences.METRIC))) {
            // locale must be NULL, otherwise doubles might be formatted with ','
            // and spaces..
            Locale locale = null;
            value.setText(String.format(locale, format, getProperty() / conversion));
        }
    }
    public double getProperty() {
        return property.getDouble();
    }

    @Override
    public void fieldChanged() {
        double val = 0.0;
        boolean valid;
        try {
            val = Double.parseDouble(value.getText());
            valid = isValid(val);
        } catch (NumberFormatException nfe) {
            valid = false;
        }
        if (valid) {
            value.setBackground(Color.WHITE);
            setProperty(val * conversion);
        } else {
            value.setBackground(Color.RED);
        }
    }
    public boolean isValid(double val) {
        return true;
    }
    public void setProperty(double val) {
        property.setDouble(val);
    }
}
