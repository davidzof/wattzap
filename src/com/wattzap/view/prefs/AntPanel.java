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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;

/**
 * Pairs ANT devices
 *
 * @author David George
 * @date 25th August 2013
 */
public class AntPanel extends JPanel implements ActionListener, MessageCallback {
    private UserPreferences userPrefs = UserPreferences.INSTANCE;

	private JTextField sandcId;
	private JTextField hrmId;
	private JLabel speedLabel;
	private JLabel hrmLabel;
    JCheckBox ant;
	JCheckBox antUSBM;

	public AntPanel() {
		super();
		MigLayout layout = new MigLayout();
		setLayout(layout);

		// TODO pair checkBox: auto-disabled when all sensors active
        JLabel sandc = new JLabel();
		sandc.setText("Speed and Cadence");
        add(sandc);
		sandcId = new JTextField(10);
        sandcId.setActionCommand("sandc");
        sandcId.addActionListener(this);
		add(sandcId);
		speedLabel = new JLabel();
		add(speedLabel, "wrap");

		JLabel hrm = new JLabel();
		hrm.setText("Heart rate");
		hrmId = new JTextField(10);
        hrmId.setActionCommand("hrm");
        hrmId.addActionListener(this);
        add(hrmId);
        hrmLabel = new JLabel();
		add(hrmLabel, "wrap");

		antUSBM = new JCheckBox("ANTUSB-m Stick");
		antUSBM.setActionCommand("antusbm");
		antUSBM.addActionListener(this);
		add(antUSBM, "wrap");

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
	}

	@Override
	public void callback(Messages message, Object o) {
		Telemetry t = (Telemetry) o;
		switch(message) {
    		case TELEMETRY:
	    		speedLabel.setText(String.format("%.1f", t.getSpeed()));
                hrmLabel.setText(String.format("%d", t.getHeartRate()));
    			break;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
	}
}
