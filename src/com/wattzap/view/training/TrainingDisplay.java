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
package com.wattzap.view.training;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartDescriptor;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.wattzap.MsgBundle;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.PauseMsgEnum;
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import java.util.List;

/**
 * (c) 2013 David George / TrainingLoops.com
 *
 * Displays training data. Shows target power/hr/cadence based on training
 * programme with real time data coming from sensors.
 *
 * @author David George
 * @date 1 September 2013
 *
 * Several improvements. Time is always counted from 0.
 * Trainings might be "recovered" without any issue (but
 * only before start! Menu option should be blocked, when
 * start button was pressed..)
 * When changing training (eg. route, mode, etc) all
 * values in the chart are shown again. "Session" is for
 * single application execution.
 * @author Jarek
 * TODO add synchronization for data.. exceptions are too often
 */
public class TrainingDisplay extends JPanel implements MessageCallback {
	private static final Logger logger = LogManager.getLogger("Training Display");
	private final UserPreferences userPrefs = UserPreferences.INSTANCE;

	private static final long MILLISECSMINUTE = 60000;
    private static final long CHARTTIMECORRECTION = 23 * 60 * 60 * 1000;

    private final List<SourceDataEnum> addedItems = new ArrayList<>();
    private SimpleXYChartSupport support = null;
	private JComponent chart;

    // Iterator<TrainingItem> training;
    private RouteReader reader = null;

    // used to save .tcx, to rebuild the chart. Kept in journal as well
    private ArrayList<Telemetry> data = null;
	private ObjectOutputStream oos = null;

    // default "session" data.
    private long startTime = 0;
    private String lastName = null;


    // last telemetry time, to ignore telemetries within one second
    private long time;

    // TODO set on config?
	boolean antEnabled = true;
    private boolean oosCreate = true;

	public TrainingDisplay(Dimension screenSize) {
		setPreferredSize(new Dimension(screenSize.width / 2, 400));
		setLayout(new BorderLayout());

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.START, this);
		MessageBus.INSTANCE.register(Messages.STOP, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);

        callback(Messages.CONFIG_CHANGED, userPrefs);
	}

    // name to be stored in history (database)
    public String getLastName() {
        return lastName;
    }

    private void addItem(SimpleXYChartDescriptor descriptor,
            SourceDataEnum item, Color color, double lineWidth) {
		descriptor.addItem(
                MsgBundle.getString(item.getName()),
                color, (float) lineWidth, color, null, null);
		addedItems.add(item);
    }

    private synchronized void rebuildChart() {
		if (chart != null) {
            addedItems.clear();
			remove(chart);
			chart = null;
		}

        // long minValue, long maxValue, long initialYMargin,
        // double chartFactor, boolean hideableItems, int valuesBuffer
        SimpleXYChartDescriptor descriptor = SimpleXYChartDescriptor.decimal(
                0, 200, 300, 1.0, true, 600);

		Color darkOrange = new Color(246, 46, 00);
        addItem(descriptor, SourceDataEnum.POWER, darkOrange, 1.0);

		if (antEnabled) {
			Color green = new Color(28, 237, 00);
            addItem(descriptor, SourceDataEnum.HEART_RATE, green, 1.0);

            addItem(descriptor, SourceDataEnum.CADENCE, Color.blue, 1.0);
		}

		if (reader != null) {
			if (reader.provides(SourceDataEnum.TARGET_POWER)) {
				Color lightOrange = new Color(255, 47, 19);
                addItem(descriptor, SourceDataEnum.TARGET_POWER, lightOrange, 2.5);
			}

			if (antEnabled) {
				if (reader.provides(SourceDataEnum.TARGET_HR)) {
					Color darkGreen = new Color(0, 110, 8);
                    addItem(descriptor, SourceDataEnum.TARGET_HR, darkGreen, 2.5);
				}

				if (reader.provides(SourceDataEnum.TARGET_CADENCE)) {
					Color lightBlue = new Color(64, 96, 255);
					addItem(descriptor, SourceDataEnum.TARGET_CADENCE, lightBlue, 2.5);
				}
			}
            /*
			descriptor
					.setDetailsItems(new String[] { "<html><font size='+2'><b>Info" });
            */
		}

		support = ChartFactory.createSimpleXYChart(descriptor);
		chart = support.getChart();
		add(chart, BorderLayout.CENTER);
		chart.setVisible(true);
		chart.revalidate();
        time = -1;

        // put current data in the chart. In "normal" telemetry time
        // starts from 0, while in ones read from journal it start
        // from startTime
        if (data != null) {
            synchronized(data) {
                for (Telemetry t : data) {
                    update(t, startTime);
                }
            }
        }
	}

	private synchronized void update(Telemetry t, long timeDiff) {
        // don't process telemetry when chart is being rebuilt (from another
        // thread for sure).
        if (chart == null) {
            logger.warn("Chart doesn't exist");
            return;
        }
        // at least power shall be shown
        // at least power shall be shown
        assert !addedItems.isEmpty() : "Nothing to be shown?";

		// use telemetry time.. timeDiff == 0 when get from sensors, etc (time
        // is how long session last), or timeDiff == startTime when telemetries
        // read from journal file.. It must be "converted" to session last.
        long resultTime = t.getTime() - timeDiff;
        if (time + 1000 >= resultTime) {
            // time doesn't advance, training is paused or something
            return;
        }

        long[] values = new long[addedItems.size()];
        int i = 0;
        for (SourceDataEnum en : addedItems) {
            if (t.isAvailable(en)) {
                values[i++] = t.getLong(en);
            } else {
                // how to indicate "non-existing" values? It there any way?
                values[i++] = 0;
            }
        }

        // description
        /*
		if ((current != null) && antEnabled) {
            String[] details = {
                    current.getDescription()
                    + current.getPowerMsg()
                    + current.getHRMsg()
                    + current.getCadenceMsg()
                    + "</b></font></html>" };
            support.updateDetails(details);
		}
        */

        time = resultTime;
        // move time a bit.. to start from 0:00:00.. But why timezone is always +1?
		support.addValues(time + CHARTTIMECORRECTION, values);
	}

	/*
	 * Save every one point for every second
     *
	 * Collection contains telemetries with "wall" time, pauses are not
     * counted..
	 * @param t
	 */
	private void add(Telemetry t) {
        if (data == null) {
            // not started?
            return;
        }

        // Telemetry provider controll messages are not added to the data
        if (PauseMsgEnum.msg(t) != null) {
            return;
        }

        // in data (and journal as well) time starts from startTime (when
        // session was started for the first time), so time must be corrected
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

    // store telemetry with "wall-clock" time
    private void storeTelemetry(Telemetry t) {
        if (oosCreate && (oos == null)) {
            oosCreate = false;
            try {
                FileOutputStream fout = new FileOutputStream(
                        userPrefs.getWD() + "/journal.ser", false);
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

	public List<Telemetry> getData() {
        if (data == null) {
            return null;
        }
        synchronized(data) {
    		return new ArrayList<Telemetry>(data);
        }
	}

    public void closeJournal() {
        if ((data == null) || (data.isEmpty())) {
			JOptionPane.showMessageDialog(this, "Logging not started yet",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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
        // clear data, this is start brand new session
        synchronized(data) {
            data.clear();
            TelemetryProvider.INSTANCE.setDistanceTime(0.0, 0);
        }
        rebuildChart();
    }

    public void loadJournal() {
        if ((oos != null) || (data != null) || (startTime != 0)) {
			JOptionPane.showMessageDialog(this, "Logging already started",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        data = new ArrayList<Telemetry>();
        synchronized(data) {
            Telemetry t = null;

            ObjectInputStream objectInputStream = null;
            try {
                FileInputStream streamIn = new FileInputStream(
                        userPrefs.getWD() + "/journal.ser");
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
                JOptionPane.showMessageDialog(this, "Recovered " + data.size()
                        + " records", "Info", JOptionPane.INFORMATION_MESSAGE);
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
                System.err.println("No data read from journal");
                data = null;
            } else {
                System.err.println("Start time " + SourceDataEnum.TIME.format((double) startTime, true));
                // restore previous location and time (for training)
                TelemetryProvider.INSTANCE.setDistanceTime(
                        t.getDistance(), t.getTime() - startTime);
            }

            // rebuild journal file from the scratch
            if (data != null) {
                for (Telemetry tt : data) {
                    storeTelemetry(tt);
                }
                rebuildChart();
            }
        }
	}

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
            Telemetry t = (Telemetry) o;
            update(t, 0);
            add(t);
			break;

        case CONFIG_CHANGED:
            UserPreferences pref = (UserPreferences) o;
            if ((pref == UserPreferences.ANT_ENABLED) ||
                    (pref == UserPreferences.INSTANCE)) {
                antEnabled = userPrefs.isAntEnabled();
                rebuildChart();
            }
            break;

		case START:
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
			if (chart == null) {
				rebuildChart();
			}
            if (data == null) {
                data = new ArrayList<Telemetry>();
            }
			break;

        case STOP:
            // decrease amount of time left.. Move to evalTimeHandler..
			if ((data != null) && (!data.isEmpty())) {
                long split;
                synchronized(data) {
                    Telemetry firstPoint = data.get(0);
                    Telemetry lastPoint = data.get(data.size() - 1);
                    split = lastPoint.getTime() - firstPoint.getTime();
                }
				int minutes = userPrefs.getEvalTime();
				minutes -= (split / MILLISECSMINUTE);
				userPrefs.setEvalTime(minutes);
			}
			break;

        // TODO replace with TRAINING LOAD
        case GPXLOAD:
            reader = (RouteReader) o;
            lastName = reader.getName();
            rebuildChart();
            break;

        case CLOSE:
			reader = null;
            rebuildChart();
			break;
        }
	}
}