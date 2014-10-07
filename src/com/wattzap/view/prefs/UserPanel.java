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

import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.UserPreferences;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// TODO: Add video directory location
public class UserPanel extends JPanel implements ActionListener, MessageCallback {
    private static final double LBSTOKG = 0.45359237;

    private UserPreferences userPrefs = UserPreferences.INSTANCE;

	// personal data
	private JTextField weight;
    private JTextField bikeWeight;
    private JTextField wheelSize;
    private JTextField maxHR;
    private JTextField maxPower;
    private JCheckBox metric;
    private JLabel weightLabel;
    private JLabel bikeWeightLabel;


	public UserPanel() {
		MigLayout layout = new MigLayout();
		setLayout(layout);

		// User Weight, label configured onConfig
		weightLabel = new JLabel();
        add(weightLabel);
		weight = new JTextField(20);
        weight.setActionCommand("weight");
        weight.addActionListener(this);
        add(weight, "span");

		// Bike weight, label configured onConfig
		bikeWeightLabel = new JLabel();
		add(bikeWeightLabel);
        bikeWeight = new JTextField(20);
        bikeWeight.setActionCommand("bike_weight");
        bikeWeight.addActionListener(this);
		add(bikeWeight, "span");

		JLabel wheelLabel = new JLabel();
		wheelLabel.setText(userPrefs.messages.getString("wheel_size") + " (mm)");
		add(wheelLabel);
		wheelSize = new JTextField(20);
        wheelSize.setActionCommand("wheel_size");
        wheelSize.addActionListener(this);
		add(wheelSize, "span");

		JLabel hrLabel = new JLabel();
		hrLabel.setText(userPrefs.messages.getString("max_hr"));
		add(hrLabel);
		maxHR = new JTextField(20);
        maxHR.setActionCommand("max_hr");
        maxHR.addActionListener(this);
		add(maxHR, "span");

		JLabel pwrLabel = new JLabel();
		pwrLabel.setText(userPrefs.messages.getString("ftp"));
		add(pwrLabel);
		maxPower = new JTextField(20);
        maxPower.setActionCommand("ftp");
        maxPower.addActionListener(this);
		add(maxPower, "span");

		metric = new JCheckBox("Metric");
        metric.setSelected(userPrefs.isMetric());
        metric.setActionCommand("metric");
        metric.addActionListener(this);
		add(metric);

        // initialize all the data
        callback(Messages.CONFIG_CHANGED, userPrefs);
	}

    private boolean changed(Object o, UserPreferences c) {
        return ((o == c) || (o == UserPreferences.INSTANCE));
    }
    private boolean changed(Object o, UserPreferences c, UserPreferences m) {
        return ((o == UserPreferences.INSTANCE) || (o == c) || (o == m));
    }


    @Override
    public void callback(Messages m, Object o) {
        if (m == Messages.CONFIG_CHANGED) {
            if (changed(o, UserPreferences.METRIC)) {
                metric.setSelected(userPrefs.isMetric());
                // set labels for weights
                if (UserPreferences.METRIC.isMetric()) {
                    weightLabel.setText(userPrefs.messages.getString("your_weight") + " (kg) ");
                    bikeWeightLabel.setText(userPrefs.messages.getString("bike_weight") + " (kg) ");
                } else {
                    weightLabel.setText(userPrefs.messages.getString("your_weight") + " (lbs)");
                    bikeWeightLabel.setText(userPrefs.messages.getString("bike_weight") + " (lbs)");
                }
            }
            if (changed(o, UserPreferences.BIKE_WEIGHT, UserPreferences.METRIC)) {
                bikeWeight.setBackground(Color.WHITE);
                bikeWeight.setText(String.format("%.1f", userPrefs.getBikeWeight()
                        / (userPrefs.isMetric() ? 1.0 :  LBSTOKG)));
            }
            if (changed(o, UserPreferences.WEIGHT, UserPreferences.METRIC)) {
                weight.setBackground(Color.WHITE);
                weight.setText(String.format("%.1f", userPrefs.getWeight()
                        / (userPrefs.isMetric() ? 1.0 :  LBSTOKG)));
            }
            if (changed(o, UserPreferences.WHEEL_SIZE)) {
                wheelSize.setBackground(Color.WHITE);
                wheelSize.setText(String.format("%d", userPrefs.getWheelsize()));
            }
            if (changed(o, UserPreferences.MAX_POWER)) {
                maxPower.setBackground(Color.WHITE);
                maxPower.setText(String.format("%d", userPrefs.getMaxPower()));
            }
            if (changed(o, UserPreferences.HR_MAX)) {
                maxHR.setBackground(Color.WHITE);
                maxHR.setText(String.format("%.1f", userPrefs.getMaxHR()));
            }
        }
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("metric")) {
            userPrefs.setMetric(metric.isSelected());
		} else if (command.equals("weight")) {
            try {
                userPrefs.setWeight(Double.parseDouble(weight.getText())
                        * (userPrefs.isMetric() ? 1.0 :  LBSTOKG));
            } catch (NumberFormatException nfe) {
                weight.setBackground(Color.RED);
            }
        } else if (command.equals("bike_weight")) {
            try {
                userPrefs.setBikeWeight(Double.parseDouble(bikeWeight.getText())
                        * (userPrefs.isMetric() ? 1.0 :  LBSTOKG));
            } catch (NumberFormatException nfe) {
                bikeWeight.setBackground(Color.RED);
            }
        } else if (command.equals("wheel_size")) {
            try {
                userPrefs.setWheelsize(Integer.parseInt(wheelSize.getText()));
            } catch (NumberFormatException nfe) {
                wheelSize.setBackground(Color.RED);
            }
        } else if (command.equals("max_power")) {
            try {
                userPrefs.setMaxPower(Integer.parseInt(maxPower.getText()));
            } catch (NumberFormatException nfe) {
                maxPower.setBackground(Color.RED);
            }
        } else if (command.equals("max_hr")) {
            try {
                userPrefs.setMaxHR(Integer.parseInt(maxHR.getText()));
            } catch (NumberFormatException nfe) {
                maxHR.setBackground(Color.RED);
            }
        }
	}
}