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

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wattzap.model.Readers;
import com.wattzap.model.RouteReader;
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
	JFrame frame;

	private static Logger logger = LogManager.getLogger("Route File Picker");

	public RouteFilePicker(JFrame panel) {
		super();
		this.frame = panel;

		List<RouteReader> readers = Readers.INSTANCE.getReaders();
		String extensions[] = new String[readers.size()];
		StringBuffer fileTypes = new StringBuffer();
		for (int i = 0; i < readers.size(); i++) {
			extensions[i] = readers.get(i).getExtension();
			if (i > 0) {
				fileTypes.append(", ");
			}
			fileTypes.append(readers.get(i).getExtension());
		}
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Supported file types (" + fileTypes.toString() + ")",
				extensions);
		setFileFilter(filter);

		File cwd = new File(UserPreferences.INSTANCE.getRouteDir());
		setCurrentDirectory(cwd);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		logger.debug(command);

		int retVal = showOpenDialog(frame);
        if (retVal == JFileChooser.APPROVE_OPTION) {
    		File file = getSelectedFile();
            RouteReader reader = Readers.readFile(file.getAbsolutePath());
            if (reader != null) {
                UserPreferences.INSTANCE.setRouteDir(file.getParent());
                UserPreferences.LAST_FILENAME.setString(file.getName());
                MessageBus.INSTANCE.send(Messages.GPXLOAD, reader);
            } else {
                logger.error("Cannot open " + file.getAbsolutePath());
                JOptionPane.showMessageDialog(frame,
                        Readers.getLastMessage() + " " + file.getAbsolutePath(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            logger.info("Open command user returned " + retVal);
        }
	}
}