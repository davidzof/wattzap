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

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.RouteReader;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TrainingItem;
import com.wattzap.model.power.Power;

/*
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class Odo extends JPanel implements MessageCallback {
    private static final double KMTOMILES = 1.609344;
    private static final double MTOFEET = 0.3048;

    private static final Color textColor = new Color(240, 244, 112);
	private static final Color skyBlue = new Color(0, 154, 237);
    private static final int style1 = Font.CENTER_BASELINE;
    private static final Font font1 = new Font("Arial", style1, 13);

    private static final int style = Font.BOLD | Font.ITALIC;
    private static final Font font = new Font("Arial", style, 30);

	private static final Logger logger = LogManager.getLogger("Odometer");
	private static final UserPreferences userPrefs = UserPreferences.INSTANCE;

    private static SimpleDateFormat timeFormat = null;
    private static boolean paused = true;

    private static final Color[] colors = new Color[] {
        Color.WHITE, skyBlue, Color.RED, Color.GRAY
    };

    private enum ValueCol {
        SPEED("speed", "%.1f", KMTOMILES),
        VSPEED("trainer_speed", "%.1f", KMTOMILES),
        DISTANCE("distance", "%.3f", KMTOMILES),
        POWER("power"),
        SLOPE("slope", "%.1f"),
        HR("heartrate"),
        CADENCE("cadence"),
        ALTITUDE("altitude", "%.0f", MTOFEET),
        RESISTANCE("resistance"),
        CHRONO("stopwatch");

        private String labelKey;
        private JLabel label;
        private String format;
        private double metricCor;
        private JLabel text;
        private int colorIndex;
        private boolean visible;
        private boolean added;

        // for integers and time
        private ValueCol(String labelKey) {
            this(labelKey, null);
        }
        // for doubles without metric handling
        private ValueCol(String labelKey, String format) {
            this(labelKey, format, 1.0);
        }
        private ValueCol(String labelKey, String format, double metricCor) {
            this.labelKey = labelKey;
            this.format = format;
            this.metricCor = metricCor;
            this.label = null;
            this.text = null;
            this.colorIndex = -1;
            this.visible = false;
            this.added = false;
        }

        public void setParams(String labelKey, String format, double metricCor) {
            if ((label != null) && (!this.labelKey.equals(labelKey))) {
                label.setText(userPrefs.messages.getString(labelKey));
            }
            this.labelKey = labelKey;
            this.format = format;
            this.metricCor = metricCor;
        }

        private void setValue(String value, int color) {
            text.setText(value);
            int index;
            if (paused) {
                index = 3;
            } else if (color > 0) {
                index = 2;
            } else if (color < 0) {
                index = 1;
            } else {
                index = 0;
            }
            if (colorIndex != index) {
                text.setForeground(colors[index]);
                this.colorIndex = index;
            }
        }
        public void setValue(int value, int color) {
            if (added) {
                setValue(String.format("%d", value), color);
            }
        }
        public void setValue(double value, int color) {
            if (added) {
                if (userPrefs.isMetric()) {
                    setValue(String.format(format, value), color);
                } else {
                    setValue(String.format(format, value / metricCor), color);
                }
            }
        }
        public void setValue(long value, int color) {
            if (added) {
                if (timeFormat == null) {
            		timeFormat = new SimpleDateFormat("HH:mm:ss");
                    timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                }
                setValue(timeFormat.format(new Date(value)), color);
            }
        }

        public void setVisible(boolean vis) {
            added = vis;
            if (added && (label == null)) {
                label = new JLabel();
                label.setFont(font1);
                label.setForeground(textColor);
                label.setText(userPrefs.messages.getString(labelKey));
                text = new JLabel();
                text.setFont(font);
            }
        }
        public static void rebuild(Odo odo) {
            ValueCol v[] = values();
            int i;
            int last = -1;
            for (i = 0; i < v.length; i++) {
                if (v[i].added) {
                    last = i;
                }
            }
            // add labels
            for (i = 0; i < v.length; i++) {
                if (v[i].added) {
                    if (i < last) {
                        odo.add(v[i].label);
                    } else {
                        odo.add(v[i].label, "Wrap");
                    }
                }
            }
            // add values
            for (i = 0; i < v.length; i++) {
                if (v[i].added) {
                    odo.add(v[i].text);
                    v[i].visible = true;
                }
            }
        }
    }



    private int routeType = RouteReader.SLOPE;
	private double totalDistance = 0;
	private TrainingItem current = null;
	Power power;

	public Odo() {
		super();

		setBackground(Color.BLACK);
		MigLayout layout = new MigLayout("fillx", "[center]", "[][shrink 0]");
		setLayout(layout);

        ValueCol.SPEED.setVisible(true);
        ValueCol.VSPEED.setVisible(true);
        ValueCol.DISTANCE.setVisible(true);
        ValueCol.POWER.setVisible(true);
        ValueCol.HR.setVisible(true);
        ValueCol.CADENCE.setVisible(true);
        ValueCol.RESISTANCE.setVisible(true);
        ValueCol.SLOPE.setVisible(true);
        ValueCol.ALTITUDE.setVisible(true);
        ValueCol.CHRONO.setVisible(true);

        Telemetry t = new Telemetry();
        callback(Messages.TELEMETRY, t);
        ValueCol.rebuild(this);

        MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.START, this);
	}

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
			Telemetry t = (Telemetry) o;
            paused = (TelemetryProvider.pauseMsg(t) != null);

            ValueCol.SPEED.setValue(t.getSpeed(), 0);
            // TODO if "simulated" speed is selected and speed sensor is
            // available, show the necessary direction
            ValueCol.VSPEED.setValue(t.getWheelSpeed(), 0);
            ValueCol.DISTANCE.setValue(t.getDistance(), 0);
            ValueCol.POWER.setValue(t.getPower(),
                    (current == null) ? 0 : current.isPowerInRange(t.getPower()));
            ValueCol.HR.setValue(t.getHeartRate(),
                    (current == null) ? 0 : current.isHRInRange(t.getHeartRate()));
            ValueCol.CADENCE.setValue(t.getCadence(),
                    (current == null) ? 0 : current.isCadenceInRange(t.getCadence()));
            // TODO if exist better resistance.. show the direction!
            ValueCol.RESISTANCE.setValue(t.getResistance(), 0);
            ValueCol.SLOPE.setValue(t.getGradient(), 0);
            ValueCol.ALTITUDE.setValue(t.getElevation(), 0);
            ValueCol.CHRONO.setValue(t.getTime(), 0);


            /*
            switch (routeType) {
                case RouteReader.POWER:
                    elevationLabel.setText(String.format("%.1f",
                        (totalDistance - t.getDistance()) / KMTOMILES));
                    break;
			}
            */
			break;



		case GPXLOAD:
            /*
			RouteReader routeData = (RouteReader) o;
			routeType = routeData.routeType();
			switch (routeType) {
                case RouteReader.POWER:
                    levelText.setText(userPrefs.messages.getString("distance_left"));
                    slopeText.setVisible(false);
                    slopeLabel.setVisible(false);
                    break;
                case RouteReader.SLOPE:
                    levelText.setText(userPrefs.messages.getString("altitude"));
                    slopeText.setVisible(true);
                    slopeLabel.setVisible(true);
                    break;
            }

			totalDistance = routeData.getDistanceMeters() / 1000.0;
            */
			break;

        case START:
			// code to see if we are registered
			if (!userPrefs.isRegistered() && (userPrefs.getEvalTime()) <= 0) {
				logger.info("Out of time " + userPrefs.getEvalTime());
				JOptionPane.showMessageDialog(this,
						userPrefs.messages.getString("trial_expired"),
						userPrefs.messages.getString("warning"),
						JOptionPane.WARNING_MESSAGE);
				userPrefs.shutDown();
				System.exit(0);
			}
		}
	}
}
