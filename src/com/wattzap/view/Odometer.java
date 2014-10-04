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
import java.text.DateFormat;
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
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;

/*
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class Odometer extends JPanel implements MessageCallback {
	private static final long serialVersionUID = -7939830514817673972L;
	private JLabel speedText;
	private JLabel distText;
	private JLabel slopeText;
	private JLabel levelText;
	private JLabel resistanceText;

	private JLabel speedLabel;
	private JLabel vspeedLabel;
	private JLabel distanceLabel;
	private JLabel elevationLabel;
	private JLabel slopeLabel;
	private JLabel resistanceLabel;

	private JLabel powerLabel;
	private JLabel chronoLabel;

	private int routeType = RouteReader.SLOPE;

	private static Logger logger = LogManager.getLogger("Odometer");

	private final Color textColor = new Color(240, 244, 112);
	private DateFormat timeFormat;
	private double totalDistance = 0;

	Power power;

	private double KMTOMILES = 1.609344;
	private final UserPreferences userPrefs = UserPreferences.INSTANCE;

	public Odometer() {
		super();

		timeFormat = new SimpleDateFormat("HH:mm:ss");
		timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		setBackground(Color.BLACK);

		int style1 = Font.CENTER_BASELINE;
		Font font1 = new Font("Arial", style1, 13);

		int style = Font.BOLD | Font.ITALIC;
		Font font = new Font("Arial", style, 30);
		MigLayout layout = new MigLayout("fillx", "[center]", "[][shrink 0]");
		this.setLayout(layout);

		// Current Speed #1
		speedText = new JLabel();
		speedText.setFont(font1);
		speedText.setForeground(textColor);
		add(speedText);

        // Virtual Speed #1
        JLabel vspeedText = new JLabel();
        vspeedText.setFont(font1);
        vspeedText.setForeground(textColor);
        vspeedText.setText(userPrefs.messages.getString("trainer_speed"));
        add(vspeedText);

		// Distance #2
		distText = new JLabel();
		distText.setFont(font1);
		distText.setForeground(textColor);
		add(distText);

		// Power #3
		JLabel pwrText = new JLabel();
		pwrText.setFont(font1);
		pwrText.setText(userPrefs.messages.getString("power"));
		pwrText.setForeground(textColor);
		add(pwrText);

		// Roller Resistance #4
		resistanceText = new JLabel();
		resistanceText.setFont(font1);
		resistanceText.setText(userPrefs.messages.getString("resistance"));
		resistanceText.setForeground(textColor);
		add(resistanceText);

		// Slope #5
		slopeText = new JLabel();
		slopeText.setFont(font1);
		slopeText.setText(userPrefs.messages.getString("slope") + " %");
		slopeText.setForeground(textColor);
		add(slopeText);

		// Altitude #6
		levelText = new JLabel();
		levelText.setFont(font1);
		levelText.setText(userPrefs.messages.getString("altitude"));
		levelText.setForeground(textColor);
		add(levelText);

		// Chrono #7
		JLabel chronoText = new JLabel();
		chronoText.setFont(font1);
		chronoText.setText(userPrefs.messages.getString("stopwatch"));
		chronoText.setForeground(textColor);
		add(chronoText, "Wrap");

		// Variables
		// #1 Speed
		speedLabel = new JLabel();
		speedLabel.setFont(font);
		speedLabel.setText("0.0");
		speedLabel.setForeground(Color.WHITE);
		add(speedLabel);

        // #2 wheelSpeed
        vspeedLabel = new JLabel();
        vspeedLabel.setFont(font);
        vspeedLabel.setText("0");
        vspeedLabel.setForeground(Color.WHITE);
        add(vspeedLabel);

		// #3 distance
		distanceLabel = new JLabel();
		distanceLabel.setFont(font);
		distanceLabel.setText("0.0");
		distanceLabel.setForeground(Color.WHITE);
		add(distanceLabel);

		// #4 power
		powerLabel = new JLabel();
		powerLabel.setFont(font);
		powerLabel.setText("0");
		powerLabel.setForeground(Color.WHITE);
		add(powerLabel);

		// #5 resistance
		resistanceLabel = new JLabel();
		resistanceLabel.setFont(font);
		resistanceLabel.setText("0");
		resistanceLabel.setForeground(Color.WHITE);
		add(resistanceLabel);

		// #6 slope
		slopeLabel = new JLabel();
		slopeLabel.setFont(font);
		slopeLabel.setText("0.0");
		slopeLabel.setForeground(Color.WHITE);
		add(slopeLabel);

		// #7 altitude
		elevationLabel = new JLabel();
		elevationLabel.setFont(font);
		elevationLabel.setForeground(Color.WHITE);
		elevationLabel.setText("0");
		add(elevationLabel);

		// #8 time
		chronoLabel = new JLabel();
		chronoLabel.setFont(font);
		chronoLabel.setForeground(Color.WHITE);
		add(chronoLabel);
		chronoLabel.setText("00:00:00");

		initLabels(userPrefs.isMetric());

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.START, this);
	}

	private void initLabels(boolean metric) {
		if (metric) {
			speedText.setText(userPrefs.messages.getString("speed") + " (km/h)");
			distText.setText(userPrefs.messages.getString("distance") + " (km)");
		} else {
			speedText.setText(userPrefs.messages.getString("speed") + " (mph)");
			distText.setText(userPrefs.messages.getString("distance") + " (miles)");
		}
	}

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
			Telemetry t = (Telemetry) o;

			// Power
			powerLabel.setText("" + t.getPower());

			// Resistance
            resistanceLabel.setText("" + t.getResistance());


			// Speed & Distance
            speedLabel.setText(String.format("%.1f", t.getSpeed() / KMTOMILES));
            distanceLabel.setText(String.format("%.3f", t.getDistance() / KMTOMILES));
            vspeedLabel.setText(String.format("%.1f", t.getWheelSpeed() / KMTOMILES));

			chronoLabel.setText(timeFormat.format(new Date(t.getTime())));

            switch (routeType) {
                case RouteReader.POWER:
                    elevationLabel.setText(String.format("%.1f",
                        (totalDistance - t.getDistance()) / KMTOMILES));
                    break;

                case RouteReader.SLOPE:
                    elevationLabel.setText(String.format("%.0f", t.getElevation()));
                    slopeLabel.setText(String.format("%.1f", t.getGradient()));
    				break;
			}
			break;



		case GPXLOAD:
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

			speedLabel.setText("0.0");
			powerLabel.setText("0");
            resistanceLabel.setText("1");
			distanceLabel.setText("0.0");
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

			initLabels(userPrefs.isMetric());
		}
	}
}
