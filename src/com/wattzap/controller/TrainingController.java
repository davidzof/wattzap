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
package com.wattzap.controller;

import com.wattzap.model.SourceDataEnum;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.WorkoutData;
import com.wattzap.utils.TcxWriter;
import com.wattzap.view.Workouts;
import com.wattzap.view.training.TrainingAnalysis;
import com.wattzap.view.training.TrainingDisplay;
import java.util.List;

/**
 * Controller linked to training data.
 *
 * (c) 2014 David George / Wattzap.com
 *
 * @author David George
 * @date 1 January 2014
 */
public class TrainingController implements ActionListener {
	private final TrainingDisplay trainingDisplay;
	private final JFrame mainFrame;
	private final TrainingAnalysis analysis = new TrainingAnalysis();

	public final static String analyze = "A";
	public final static String save = "S";
	public final static String recover = "R";
	public final static String view = "V";
	public final static String open = "O";
	public final static String start = "B";
	public final static String stop = "E";
    public final static String clear = "C";

	Workouts workouts = null;

	public TrainingController(TrainingDisplay trainingDisplay, JFrame frame) {
		this.trainingDisplay = trainingDisplay;
		mainFrame = frame;
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

        if (start.equals(command)) {
            MessageBus.INSTANCE.send(Messages.START, null);
        } else if (stop.equals(command)) {
            MessageBus.INSTANCE.send(Messages.STOP, null);
        } else if (save.equals(command)) {
			List<Telemetry> data = trainingDisplay.getData();
			if ((data == null) || (data.size() == 0)) {
                JOptionPane.showMessageDialog(mainFrame, "No training data available",
                        "No data", JOptionPane.WARNING_MESSAGE);
				return;
			}

			boolean withGpsData = false;
			Telemetry zero = data.get(0);
			if (zero.isAvailable(SourceDataEnum.LATITUDE)) {
				// gpsData == 0 is Yes
				withGpsData = (JOptionPane.showConfirmDialog(mainFrame,
						"Save with GPS and Altitude data?", "GPS Data",
						JOptionPane.YES_NO_OPTION) == 0);
			}

            TcxWriter writer = new TcxWriter();
			String fileName = writer.save(data, withGpsData);
			WorkoutData workoutData = TrainingAnalysis.analyze(data);
			workoutData.setTcxFile(fileName);
			workoutData.setFtp(UserPreferences.INSTANCE.getMaxPower());
			workoutData.setDescription(trainingDisplay.getLastName());
			UserPreferences.INSTANCE.addWorkout(workoutData);
            MessageBus.INSTANCE.send(Messages.WORKOUT_DATA, workoutData);

			JOptionPane.showMessageDialog(mainFrame, "Saved workout to "
					+ fileName, "Workout Saved",
					JOptionPane.INFORMATION_MESSAGE);

		} else if (analyze.equals(command)) {
			List<Telemetry> data = trainingDisplay.getData();
			WorkoutData wData = TrainingAnalysis.analyze(data);
			if (wData != null) {
				wData.setFtp(UserPreferences.INSTANCE.getMaxPower());
				analysis.show(wData);
			}
        } else if (clear.equals(command)) {
			trainingDisplay.closeJournal();
		} else if (recover.equals(command)) {
			// recover data
			trainingDisplay.loadJournal();
		} else if (view.equals(command)) {
			if (workouts == null) {
				workouts = new Workouts();
			} else {
				workouts.setVisible(true);
			}
		}
	}
}