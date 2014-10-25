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
package com.wattzap.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wattzap.model.Readers;
import com.wattzap.model.UserPreferences;
import javax.swing.JOptionPane;

/**
 * (c) 2013 David George / Wattzap.com
 *
 * Speed and Cadence ANT+ processor.
 *
 * @author David George
 * @date 11 June 2013
 */
public class RouteFilePicker extends JFileChooser implements ActionListener {
	private static Logger logger = LogManager.getLogger("Route File Picker");

	private final JFrame frame;

	public RouteFilePicker(JFrame panel) {
		super();
		this.frame = panel;

		// configure fileSelection panel
        String extensions[] = Readers.getExtensions();
		StringBuffer fileTypes = new StringBuffer();
		for (int i = 0; i < extensions.length; i++) {
			if (i != 0) {
				fileTypes.append(", ");
			}
			fileTypes.append(extensions[i]);
		}
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Supported file types (" + fileTypes.toString() + ")",
				extensions);
		setFileFilter(filter);

		File last = new File(UserPreferences.INSTANCE.getDefaultFilename());
		setCurrentDirectory(last.getParentFile());
	}

	/*
	 * A file was selected by the user
	 */
	@Override
	public final void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		int retVal = showOpenDialog(frame);
        if (retVal == JFileChooser.APPROVE_OPTION) {
    		File file = getSelectedFile();
            String msg = Readers.runTraining(file.getAbsolutePath());
            // if proper file.. store path as new "default" training. Otherwise
            // current training stays still loaded.
            if (msg == null) {
                UserPreferences.LAST_FILENAME.setString(file.getAbsolutePath());
            } else {
                logger.error("Cannot open " + file.getAbsolutePath() + ":: " + msg);
                JOptionPane.showMessageDialog(frame,
                        msg + " " + file.getAbsolutePath(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            logger.info("Open command user returned " + retVal);
        }
	}
}