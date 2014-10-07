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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import com.wattzap.model.UserPreferences;

public class Preferences extends JFrame implements ActionListener {
    private static final UserPreferences userPrefs = UserPreferences.INSTANCE;

	private UserPanel userPanel;
    private TurboPanel trainerPanel;
    private AntPanel antPanel;

	public Preferences() {
		setTitle("Preferences");
		ImageIcon img = new ImageIcon("icons/preferences.jpg");
		setIconImage(img.getImage());

		JTabbedPane jtp = new JTabbedPane();

		Container contentPane = getContentPane();
		// MigLayout layout1 = new MigLayout();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(jtp, BorderLayout.CENTER);
		contentPane.setBackground(Color.lightGray);

		// Personal Data
        userPanel = new UserPanel();
        jtp.addTab(userPrefs.messages.getString("personal_data"), userPanel);

		// ANT+ Pairing
		antPanel = new AntPanel();
        jtp.addTab("ANT+", antPanel);

		// Trainer Profiles
		trainerPanel = new TurboPanel();
		jtp.addTab("Trainer", trainerPanel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		JButton saveButton = new JButton(userPrefs.messages.getString("saveclose"));
		saveButton.setPreferredSize(new Dimension(150, 30));
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);

		JButton cancelButton = new JButton(userPrefs.messages.getString("cancel"));
		cancelButton.setPreferredSize(new Dimension(120, 30));
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		contentPane.add(buttonPanel, BorderLayout.SOUTH);

		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("save".equals(command)) {
			setVisible(false); // you can't see me!
			dispose(); // Destroy the JFrame object
			return;
		} else if ("cancel".equals(command)) {
			setVisible(false); // you can't see me!
			dispose(); // Destroy the JFrame object
			return;
		}
	}
}