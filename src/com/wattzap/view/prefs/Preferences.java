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

import com.wattzap.MsgBundle;
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

import java.awt.FlowLayout;
import javax.swing.JPanel;

/*
 * Preferences panel. It creates all tabbed panels to
 * handle all sorts of configuration data.
 *
 * @author Jarek
 */

public class Preferences extends JFrame implements ActionListener {

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
        jtp.addTab(MsgBundle.getString("personal_data"), new UserPanel());
		// ANT+ Pairing
        jtp.addTab(MsgBundle.getString("sensors"), new SensorsPanel());
		// Trainer Profiles
		jtp.addTab(MsgBundle.getString("trainers"), new TurboPanel());
        // data sources, and their config
        jtp.addTab(MsgBundle.getString("sources"), new SourcesPanel());

        JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());

		JButton closeButton = new JButton(MsgBundle.getString("close"));
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
        }
		if ("close".equals(command)) {
			setVisible(false); // you can't see me!
			dispose();
		}
	}
}