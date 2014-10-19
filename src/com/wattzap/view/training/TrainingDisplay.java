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
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartDescriptor;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TelemetryValidityEnum;
import com.wattzap.model.dto.TrainingData;
import com.wattzap.model.dto.TrainingItem;
import java.util.List;

/**
 * (c) 2013 David George / TrainingLoops.com
 *
 * Speed and Cadence ANT+ processor.
 *
 * @author David George
 * @date 1 September 2013
 */
public class TrainingDisplay extends JPanel implements MessageCallback {
	private SimpleXYChartSupport support = null;

	Iterator<TrainingItem> training;
	TrainingData tData;
	TrainingItem current;
	private ArrayList<Telemetry> data;
	JComponent chart = null;
	ObjectOutputStream oos = null;
	boolean antEnabled = true;

	private static final long MILLISECSMINUTE = 60000;
    private static final long CHARTTIMECORRECTION = 23 * 60 * 60 * 1000;

	private final UserPreferences userPrefs = UserPreferences.INSTANCE;

	private static Logger logger = LogManager.getLogger("Training Display");

    // new way..
    private final List<SourceDataEnum> addedItems = new ArrayList<>();
    private long time;
    // TODO set on config?
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

    private void addItem(SimpleXYChartDescriptor descriptor,
            SourceDataEnum item, Color color, double lineWidth) {
		descriptor.addItem(
                userPrefs.messages.getString(item.getName()),
                color, (float) lineWidth, color, null, null);
		addedItems.add(item);
        System.err.println("Adding item " + item.getName());
    }

    private synchronized void rebuildChart() {
		if (chart != null) {
            System.err.println("Remove previous chart");
            addedItems.clear();
			remove(chart);
			chart = null;
		}

        System.err.println("Build chart");

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

		if (tData != null) {
			if (tData.isPwr()) {
				Color lightOrange = new Color(255, 47, 19);
                addItem(descriptor, SourceDataEnum.TARGET_POWER, lightOrange, 2.5);
			}

			if (antEnabled) {
				if (tData.isHr()) {
					Color darkGreen = new Color(0, 110, 8);
                    addItem(descriptor, SourceDataEnum.TARGET_HR, darkGreen, 2.5);
				}

				if (tData.isCdc()) {
					Color lightBlue = new Color(64, 96, 255);
					addItem(descriptor, SourceDataEnum.TARGET_CADENCE, lightBlue, 2.5);
				}
			}
			descriptor
					.setDetailsItems(new String[] { "<html><font size='+2'><b>Info" });
		}

		support = ChartFactory.createSimpleXYChart(descriptor);
		chart = support.getChart();
		add(chart, BorderLayout.CENTER);
		chart.setVisible(true);
		chart.revalidate();
        time = -1;

        // put current data in the chart
        if (data != null) {
            for (Telemetry t : data) {
                update(t);
            }
        }

        System.err.println("Chart built");
	}

	private void update(Telemetry t) {
        synchronized(this) {
            if (chart == null) {
                System.err.println("Chart doesn't exist");
                return;
            }
        }

        if (addedItems.isEmpty()) {
            System.err.println("Nothing to be shown?");
            return;
        }

        if (time == t.getTime()) {
            // time doesnt' advance, training is paused
            return;
        }

        long[] values = new long[addedItems.size()];
        int i = 0;
        for (SourceDataEnum en : addedItems) {
            if ((t.getValidity(en) != TelemetryValidityEnum.NOT_PRESENT) &&
                    (t.getValidity(en) != TelemetryValidityEnum.NOT_AVAILABLE)) {
                values[i++] = t.getLong(en);
            } else {
                values[i++] = -1;
            }
        }

        // description
		if ((current != null) && antEnabled && (tData != null)) {
            String[] details = {
                    current.getDescription()
                    + current.getPowerMsg()
                    + current.getHRMsg()
                    + current.getCadenceMsg()
                    + "</b></font></html>" };
            support.updateDetails(details);
		}

		// use telemetry time
        time = t.getTime();
        // move time a bit.. to start from 0:00:00
		support.addValues(time + CHARTTIMECORRECTION, values);
	}

	/*
	 * Save every one point for every second TODO: move this to data acquisition
	 * so we don't even send this points
	 *
	 * @param t
	 */
	private void add(Telemetry t) {
        if (data != null) {
            // don't add telemetry too often..
            if (!data.isEmpty()) {
                Telemetry tn = data.get(data.size() - 1);
                if (t.getTime() < tn.getTime() + 1000) {
                    return;
                }
            }
            data.add(t);
        }

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

	public ArrayList<Telemetry> getData() {
		return data;
	}

    public void loadJournal() {
        if (oos != null) {
			JOptionPane.showMessageDialog(this, "Logging already started",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

		ObjectInputStream objectinputstream = null;
		List<Telemetry> data = new ArrayList<Telemetry>();
		Telemetry t = null;
		try {
			FileInputStream streamIn = new FileInputStream(
                    userPrefs.getWD() + "/journal.ser");
			objectinputstream = new ObjectInputStream(streamIn);

			while ((t = (Telemetry) objectinputstream.readObject()) != null) {
				data.add(t);
                update(t);
			}
		} catch (EOFException ex) {
            // nothing
		} catch (Exception e) {
			logger.error(e + ":: cannot read " + e.getLocalizedMessage());
		} finally {
            logger.debug("read " + data.size() + " records");
			JOptionPane.showMessageDialog(this, "Recovered " + data.size()
					+ " records", "Info", JOptionPane.INFORMATION_MESSAGE);
			if (objectinputstream != null) {
				try {
					objectinputstream.close();
				} catch (IOException e) {
					logger.error(e + ":: Cannot close " + e.getLocalizedMessage());
				}
			}
		}

        // restore previous location
        if (t != null) {
            TelemetryProvider.INSTANCE.setDistanceTime(t.getDistance(), t.getTime());
        }

        // rebuild telemetry data, journal file is created from scratch
        this.data = new ArrayList<>();
        for (Telemetry tt : data) {
            add(tt);
        }
        rebuildChart();
	}

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
            Telemetry t = (Telemetry) o;
            update(t);
            add(t);
			break;

        case CONFIG_CHANGED:
            antEnabled = userPrefs.isAntEnabled();
            rebuildChart();
            break;

		case START:
            System.err.println("Start!");
			if (chart == null) {
				rebuildChart();
			}
            if (data == null) {
                data = new ArrayList<Telemetry>();
            }
			break;

        case STOP:
            System.err.println("Stop!");
            // decrease amount of time left.. Move to evalTimeHandler..
			if ((data != null) && (!data.isEmpty())) {
                Telemetry firstPoint = data.get(0);
				Telemetry lastPoint = data.get(data.size() - 1);
				long split = lastPoint.getTime() - firstPoint.getTime();
				int minutes = userPrefs.getEvalTime();
				minutes -= (split / MILLISECSMINUTE);
				userPrefs.setEvalTime(minutes);
			}
			break;

        // TODO TRAINING LOAD
        case GPXLOAD:
            System.err.println("GPX Load!");
            // TODO tData = o;
            rebuildChart();
            break;

        case CLOSE:
            System.err.println("Close!");
			tData = null;
            rebuildChart();
			break;

        /* TODO what does it mean?
        case STARTPOS:
			double distance = (Double) o;
			if (current != null && current.getTime() == 0 && tData != null) {
				training = tData.getTraining().iterator();

				// Power Program
				TrainingItem item = current = training.next();
				while (current.getDistance() < distance) {
					if (training.hasNext()) {
						current = training.next();
					}
					item = current;
				}
				current = item;
			}

			break;

        case TRAINING:
			tData = (TrainingData) o;

			training = tData.getTraining().iterator();
			if (training.hasNext()) {
				current = training.next();

				MessageBus.INSTANCE.send(Messages.TRAININGITEM, current);
			}

			buildChart();
			// data = new ArrayList<Telemetry>();
			break;
        */
        default:
            System.err.println("Unhandled message " + message);
        }
	}
}