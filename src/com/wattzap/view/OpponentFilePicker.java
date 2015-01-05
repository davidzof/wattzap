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

import com.wattzap.PopupMessageIntf;
import com.wattzap.model.Opponents;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wattzap.model.Readers;
import com.wattzap.model.UserPreferences;

/**
 * (c) 2013 David George / Wattzap.com
 *
 * Speed and Cadence ANT+ processor.
 *
 * @author David George
 * @date 11 June 2013
 */
public class OpponentFilePicker extends JFileChooser implements ActionListener {
	private static Logger logger = LogManager.getLogger("Route File Picker");

	private final JFrame frame;
    private final PopupMessageIntf popup;

	public OpponentFilePicker(JFrame panel, PopupMessageIntf popup) {
		super();
		this.frame = panel;
        this.popup = popup;

		// configure fileSelection panel
        setFileFilter(Readers.getExtensionFilter());

		File last = new File(UserPreferences.INSTANCE.getDefaultFilename());
        if (last.exists()) {
    		setCurrentDirectory(last.getParentFile());
        } else {
            setCurrentDirectory(new File(UserPreferences.INSTANCE.getTrainingDir()));
        }
	}

	/*
	 * A file was selected by the user
	 */
	@Override
	public final void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("opponent".equals(command)) {
			Opponents.removeOpponents();
        }
		int retVal = showOpenDialog(frame);
        if (retVal == JFileChooser.APPROVE_OPTION) {
    		String file = getSelectedFile().getAbsolutePath();
            int id = Opponents.addOpponent(Readers.createReader(file, popup));
            if (id == 0) {
                logger.warn("Cannot create opponent, " + file);
            } else {
                // UserPreferences.LAST_FILENAME.setString(file);
            }
        } else {
            logger.info("Open command user returned " + retVal);
        }
	}
}