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

import com.wattzap.PopupMessage;
import com.wattzap.model.PauseMsgEnum;
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.TelemetryProvider;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.WorkoutData;
import com.wattzap.utils.TcxWriter;
import com.wattzap.view.Workouts;
import com.wattzap.view.training.TrainingAnalysis;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Controller linked to training data.
 *
 * (c) 2014 David George / Wattzap.com
 *
 * @author David George
 * @date 1 January 2014
 *
 * It handleds telemetry data, stores it in journal file and handles other basic
 * operations.
 */
public class TrainingController implements ActionListener, MessageCallback {
	private static final Logger logger = LogManager.getLogger("TrainingCtrl");
	private static final long MILLISECSMINUTE = 60000;

	public final static String analyze = "A";
	public final static String save = "S";
	public final static String recover = "R";
	public final static String view = "V";
	public final static String open = "O";
	public final static String start = "B";
	public final static String stop = "E";
    public final static String clear = "C";
    public final static String pause = "P";

    // telemetries, starts from starting time (not from 0 as in telemetries)
    private final List<Telemetry> data = new ArrayList<>();

    private final PopupMessage popup;

    // journal file with "stored" data. Used by "recover" action in case of
    // fatal-exit condition.
    private ObjectOutputStream oos = null;
    private boolean oosCreate = true;

    // Name of last training file. Stored in DB when saved.
    private String lastName = null;

    // time (wallClock) when training was started for the first time
    private long startTime = 0;

    private TrainingAnalysis analysis = null;
	private Workouts workouts = null;

    public TrainingController(PopupMessage popup) {
		this.popup = popup;

        // store all telemetries
		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
        // handle requests for telemetry data
        MessageBus.INSTANCE.register(Messages.TD_REQ, this);
        // to get name for the file, this name is stored in the database
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
        // clean up.. App is finishing..
        MessageBus.INSTANCE.register(Messages.EXIT_APP, this);
	}

    @Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

        // replace with java 7 (?) switch/case?
        if (start.equals(command)) {
            MessageBus.INSTANCE.send(Messages.START, null);

        } else if (stop.equals(command)) {
            MessageBus.INSTANCE.send(Messages.STOP, null);

        } else if (save.equals(command)) {
            saveWorkout(popup);

		} else if (analyze.equals(command)) {
			List<Telemetry> data = getData();
			WorkoutData wData = TrainingAnalysis.analyze(data);
			if (wData != null) {
				wData.setFtp(UserPreferences.INSTANCE.getMaxPower());
                if (analysis == null) {
                    analysis = new TrainingAnalysis();
                }
				analysis.show(wData);
			}

        } else if (clear.equals(command)) {
			clearJournal(popup);

        } else if (recover.equals(command)) {
			// recover data
			loadJournal(popup);

        } else if (pause.equals(command)) {
            UserPreferences.MANUAL_PAUSE.setPaused(!UserPreferences.INSTANCE.isPaused());

        } else if (view.equals(command)) {
			if (workouts == null) {
				workouts = new Workouts();
			} else {
				workouts.setVisible(true);
			}
		}
	}

	public List<Telemetry> getData() {
        synchronized(data) {
    		return new ArrayList<Telemetry>(data);
        }
	}

    /*
	 * Save every one point for every second
     *
	 * Collection contains telemetries with "wall" time, pauses are not
     * counted..
	 * @param t
	 */
	private void add(Telemetry t) {
        // TelemetryProvider controll messages are not added to the data
        if (PauseMsgEnum.msg(t) != null) {
            return;
        }

        // current time is start time in case of first update
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        // in data (and journal as well) time starts from startTime (when
        // session was started for the first time), so time must be corrected.
        // Pauses are not include in the data, only slow-downs are recorded
        // (but is impossible to check how long training was paused).
        long time = t.getTime() + startTime;

        // don't add telemetry too often..
        Telemetry tt;
        synchronized(data) {
            if (!data.isEmpty()) {
                Telemetry tn = data.get(data.size() - 1);
                if (time < tn.getTime() + 1000) {
                    return;
                }
            }
            tt = new Telemetry(t);
            tt.setTime(time);
            data.add(tt);
        }
        storeTelemetry(tt);
    }

    private static String getJournalName() {
        return UserPreferences.getWD() + "/journal.ser";
    }

    // store telemetry with "wall-clock" time
    private void storeTelemetry(Telemetry t) {
        if (oosCreate && (oos == null)) {
            oosCreate = false;
            try {
                FileOutputStream fout = new FileOutputStream(
                        getJournalName(), false);
                oos = new ObjectOutputStream(fout);
            } catch (Exception e) {
                logger.error(e + ":: Can't create journal file "
                        + e.getLocalizedMessage());
            }
        }
        if (oos != null) {
            try {
                oos.writeObject(t);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.error(e + "Can't write telemetry data to journal "
                        + e.getLocalizedMessage());
            }
        }
    }

    public void saveWorkout(PopupMessage popup) {
        List<Telemetry> data = getData();
        if (data.isEmpty()) {
            if (popup != null) {
                popup.showWarning("No data", "No training data available");
            }
            return;
        }

        boolean withGpsData = false;
        Telemetry zero = data.get(0);
        if (zero.isAvailable(SourceDataEnum.LATITUDE)) {
            if (popup != null) {
                withGpsData = popup.confirm("GPS Data", "Save with GPS and Altitude data?");
            } else {
                withGpsData = true;
            }
        }

        TcxWriter writer = new TcxWriter();
        String fileName = writer.save(data, withGpsData);
        logger.debug("Save workout to " + fileName);
        WorkoutData workoutData = TrainingAnalysis.analyze(data);
        workoutData.setTcxFile(fileName);
        workoutData.setFtp(UserPreferences.INSTANCE.getMaxPower());
        workoutData.setDescription(lastName);
        UserPreferences.INSTANCE.addWorkout(workoutData);
        // send notification with changed workout. It refreshes list of
        // workouts.
        MessageBus.INSTANCE.send(Messages.WORKOUT_DATA, workoutData);

        if (popup != null) {
            popup.showMessage("Workout Saved", "Saved workout to " + fileName);
        }
    }

    public void clearJournal(PopupMessage popup) {
        boolean noData = true;
        synchronized(data) {
            noData = data.isEmpty();
            if (!noData) {
                // close oos if exists
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                        logger.error("Can't close journal file "
                                + e.getLocalizedMessage());
                    }
                    oos = null;
                }
                File journal = new File(getJournalName());
                if (!journal.delete()) {
                    logger.error("Cannot delte journal file");
                }
                data.clear();
            }
        }
        if (noData) {
            if (popup != null) {
                popup.showMessage("Info", "Logging not started yet");
            }
        } else {
            // start "brand new" session
            TelemetryProvider.INSTANCE.setDistanceTime(0.0, 0);
            // and updata "new" data in all interfaces
            MessageBus.INSTANCE.send(Messages.TD, getData());
        }
    }

    public void loadJournal(PopupMessage popup) {
        if ((oos != null) || (startTime != 0)) {
            if (popup != null) {
                popup.showMessage("Info", "Logging already started");
            }
            return;
        }

        int entries = 0;
        synchronized(data) {
            data.clear();
            Telemetry t = null;

            ObjectInputStream objectInputStream = null;
            try {
                FileInputStream streamIn = new FileInputStream(
                        getJournalName());
                objectInputStream = new ObjectInputStream(streamIn);

                while ((t = (Telemetry) objectInputStream.readObject()) != null) {
                    data.add(t);
                    if (startTime == 0) {
                        startTime = t.getTime();
                    }
                }
            } catch (EOFException ex) {
                // nothing.. just normal response..
            } catch (Exception e) {
                logger.error(e + ":: cannot read " + e.getLocalizedMessage());
            } finally {
                logger.debug("read " + data.size() + " records");
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        logger.error(e + ":: Cannot close " + e.getLocalizedMessage());
                    }
                }
            }

            if (t == null) {
                // nothing was read.. pitty
                logger.debug("No data read from journal");
                entries = 0;
            } else {
                logger.debug("Start time " + SourceDataEnum.TIME.format((double) startTime, true));
                // restore previous location and time (for training)
                TelemetryProvider.INSTANCE.setDistanceTime(
                        t.getDistance(), t.getTime() - startTime);
                entries = data.size();
            }

            // rebuild journal file from the scratch.. sometimes it is broken,
            // thus cannot append to the existing file
            if (data != null) {
                for (Telemetry tt : data) {
                    storeTelemetry(tt);
                }
            }
        }

        if (popup != null) {
            if (entries != 0) {
                popup.showMessage("Info", "Recovered " + entries + " records");
            } else {
                popup.showWarning("Info", "Cannot recover any data");
            }
        }
        MessageBus.INSTANCE.send(Messages.TD, getData());
	}

    @Override
    public void callback(Messages m, Object o) {
        switch (m) {
 		case TELEMETRY:
            Telemetry t = (Telemetry) o;
            add(t);
			break;

        // training data was requested (by TrainingDisplay). Send current data
        // to the requester
        case TD_REQ:
            MessageCallback display = (MessageCallback) o;
            display.callback(Messages.TD, getData());
            break;

        // TODO replace with TRAINING LOAD
        case GPXLOAD:
            RouteReader reader = (RouteReader) o;
            lastName = reader.getName();
            break;

        case EXIT_APP:
            // decrease app evalTime.. Several scenarios are to be considered:
            // - journalFile loaded and telemetry starts from "non-zero" time
            // - journalFile cleared.. and some time is discarded
            // - application hangs up.. and time was not modified
            /*
            int minutes = UserPreferences.EVAL_TIME.getEvalTime();
            minutes -= (0 / MILLISECSMINUTE);
            UserPreferences.EVAL_TIME.setEvalTime(minutes);
            */

            MessageBus.INSTANCE.send(Messages.STOP, null);
            // close training file
            MessageBus.INSTANCE.send(Messages.CLOSE, null);

            // save current training and clear journal.
            if (UserPreferences.AUTO_SAVE.autosave()) {
                saveWorkout(null);
                clearJournal(null);
            }
            break;
        }
    }
}