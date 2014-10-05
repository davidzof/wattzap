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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import com.wattzap.model.UserPreferences;
import com.wattzap.model.VirtualPowerEnum;
import com.wattzap.model.power.Power;
import com.wattzap.model.power.PowerProfiles;

/*
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class TurboPanel extends JPanel implements ActionListener {
	private final UserPreferences userPrefs = UserPreferences.INSTANCE;

    private final List<JRadioButton> trainerProfiles = new ArrayList<>();
	private JComboBox virtualPower;
	private JComboBox resistanceLevels = null;

	public TurboPanel() {
		super();

		MigLayout layout = new MigLayout();
		setLayout(layout);
		JLabel label2 = new JLabel();
		label2.setText("Select your Profile");
		add(label2, "wrap");

		// Create the radio buttons.
		PowerProfiles pp = PowerProfiles.INSTANCE;
		List<Power> profiles = pp.getProfiles();

        ButtonGroup group = new ButtonGroup();
		String trainerDescription = userPrefs.getPowerProfile().description();
		Power selectedProfile = null;
		for (Power p : profiles) {
			JRadioButton button = new JRadioButton(p.description());

			if (p.description().equals(trainerDescription)) {
				button.setEnabled(true);
			}

			trainerProfiles.add(button);
			button.setActionCommand("trainer");
			// button.setSelected(true);
			group.add(button);
			add(button, "wrap");
			button.addActionListener(this);
			if (p.description().equals(trainerDescription)) {
				button.setSelected(true);
				selectedProfile = p;
			}
		}

		// power profiles
        virtualPower = new JComboBox();
        for (VirtualPowerEnum e : VirtualPowerEnum.values()) {
            if (userPrefs.messages.containsKey(e.getKey())) {
                virtualPower.addItem(userPrefs.messages.getString(e.getKey()));
            } else {
                virtualPower.addItem(e.getKey());
            }
        }
        virtualPower.setSelectedIndex(userPrefs.getVirtualPower().ordinal());
		add(virtualPower, "wrap");
        virtualPower.setActionCommand("virtualPower");
		virtualPower.addActionListener(this);

        // resistance
        displayResistance(selectedProfile);
	}

    private void displayResistance(Power p) {
		if (resistanceLevels != null) {
			remove(resistanceLevels);
			resistanceLevels = null;
		}
		if (p != null && p.getResitanceLevels() > 1) {
			resistanceLevels = new JComboBox();

            // auto is always visible: some trainers are able to set resistance
            // on requested load (this is.. when wheel speed best match
            // real speed.
            resistanceLevels.addItem("auto");

			for (int i = 1; i <= p.getResitanceLevels(); i++) {
				resistanceLevels.addItem("" + i);
			}
			if (p.description().equals(userPrefs.getPowerProfile().description())) {
				resistanceLevels.setSelectedIndex(userPrefs.getResistance());
			}
			add(resistanceLevels);
		}
        validate();
		repaint();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

        if (command.equals("trainer")) {
            String d = getProfileDescription();
            PowerProfiles pp = PowerProfiles.INSTANCE;
            Power p = pp.getProfile(d);
            displayResistance(p);
            userPrefs.setPowerProfile(d);

        } else if (command.equals("virtualPower")) {
            userPrefs.setVirtualPower(
                    VirtualPowerEnum.values()[resistanceLevels.getSelectedIndex()]);
        }
	}

    private String getProfileDescription() {
		for (JRadioButton button : trainerProfiles) {
			if (button.isSelected()) {
				return button.getText();
			}
		}
		return null;
	}
}
