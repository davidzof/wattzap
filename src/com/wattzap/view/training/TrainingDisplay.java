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
import java.util.ArrayList;

import javax.swing.JComponent;
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
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import java.awt.Toolkit;
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
 */
public class TrainingDisplay extends JPanel implements MessageCallback {
	private static final Logger logger = LogManager.getLogger("Training Display");
	private static final UserPreferences userPrefs = UserPreferences.INSTANCE;

    private static final long CHARTTIMECORRECTION = 23 * 60 * 60 * 1000;

    private final List<SourceDataEnum> addedItems = new ArrayList<>();
    private SimpleXYChartDescriptor descriptor = null;
    private SimpleXYChartSupport support = null;
	private JComponent chart = null;
    private long time;

    private RouteReader reader = null;
	boolean antEnabled = true;

	public TrainingDisplay() {
        // why the size is as given? I thing about 2/3rd of parent window
        // (then it will be moved to MainFrame)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(new Dimension(screenSize.width / 2, 400));

        setLayout(new BorderLayout());

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.TD, this);
		MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);

        // rebuild imediatelly
        callback(Messages.CONFIG_CHANGED, userPrefs);
	}

    private void addItem(SimpleXYChartDescriptor descriptor,
            SourceDataEnum item, Color color, double lineWidth) {
		descriptor.addItem(
                MsgBundle.getString(item.getName()),
                color, (float) lineWidth, color, null, null);
		addedItems.add(item);
    }

    private synchronized void rebuildChart() {
        addedItems.clear();

        // long minValue, long maxValue, long initialYMargin,
        // double chartFactor, boolean hideableItems, int valuesBuffer
        descriptor = SimpleXYChartDescriptor.decimal(
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

        time = -1;

        // request all training data
        MessageBus.INSTANCE.send(Messages.TD_REQ, this);
    }

    // Put current data in the chart.
    private synchronized void rebuildData(List<Telemetry> data) {
		if (chart != null) {
			remove(chart);
			chart = null;
		}

        support = ChartFactory.createSimpleXYChart(descriptor);
		chart = support.getChart();
		add(chart, BorderLayout.CENTER);
		chart.setVisible(true);
		chart.revalidate();

        if ((data != null) && (!data.isEmpty())) {
            logger.debug("Update " + data.size() + " points in training chart.");
            // In "normal" telemetry time starts from 0, while in ones read from
            // journal it start from startTime
            long startTime = data.get(0).getTime();
            for (Telemetry t : data) {
                update(t, startTime);
            }
        } else {
            logger.debug("No data, show 0:0 time in training chart");
            long[] values = new long[addedItems.size()];
            for (int i = 0; i < addedItems.size(); i++) {
                values[i] = 0;
            }
            // reset axis to 0:00
            support.addValues(CHARTTIMECORRECTION, values);
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
        // move time a bit.. to start from 0:00:00.. Time "starts" from
        // Jan 1st 1960, 1:00, I wonder why.. So for display purposes Jan 2nd
        // is better
		support.addValues(time + CHARTTIMECORRECTION, values);
	}


	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
            Telemetry t = (Telemetry) o;
            update(t, 0);
			break;

        case CONFIG_CHANGED:
            UserPreferences pref = (UserPreferences) o;
            if ((pref == UserPreferences.ANT_ENABLED) ||
                    (pref == UserPreferences.INSTANCE)) {
                antEnabled = userPrefs.isAntEnabled();
                rebuildChart();
            }
            break;

        // TODO replace with TRAINING LOAD
        case GPXLOAD:
            reader = (RouteReader) o;
            rebuildChart();
            break;

        case CLOSE:
			reader = null;
            rebuildChart();
			break;

        case TD:
            rebuildData((List<Telemetry>) o);
			break;
        }
	}
}