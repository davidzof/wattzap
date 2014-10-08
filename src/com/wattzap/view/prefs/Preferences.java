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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import com.wattzap.model.UserPreferences;
import java.awt.FlowLayout;
import javax.swing.JPanel;

public class Preferences extends JFrame implements ActionListener {
    private static final UserPreferences userPrefs = UserPreferences.INSTANCE;

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
        jtp.addTab(userPrefs.messages.getString("personal_data"), new UserPanel());
		// ANT+ Pairing
        jtp.addTab("ANT+", new AntPanel());
		// Trainer Profiles
		jtp.addTab("Trainer", new TurboPanel());

        JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());

		JButton closeButton = new JButton(userPrefs.messages.getString("close"));
		closeButton.setPreferredSize(new Dimension(120, 30));
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);

        buttonPanel.add(closeButton);
		contentPane.add(buttonPanel, BorderLayout.SOUTH);

		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
        if ("Preferences".equals(command)) {
			setVisible(true); // you can see me (again)!
            return;
        }
		if ("close".equals(command)) {
			setVisible(false); // you can't see me!
			dispose();
			return;
		}
	}
}