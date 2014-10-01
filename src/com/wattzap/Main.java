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
package com.wattzap;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.omniscient.log4jcontrib.swingappender.SwingAppender;
import com.sun.jna.NativeLibrary;
import com.wattzap.controller.MenuItem;
import com.wattzap.controller.Messages;
import com.wattzap.controller.TrainingController;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.ant.AntSubsystem;
import com.wattzap.model.ant.DummySpeedCadenceListener;
import com.wattzap.model.ant.SpeedAndCadenceSensor;
import com.wattzap.view.AboutPanel;
import com.wattzap.view.AntOdometer;
import com.wattzap.view.ControlPanel;
import com.wattzap.view.MainFrame;
import com.wattzap.view.Map;
import com.wattzap.view.Odometer;
import com.wattzap.view.Profile;
import com.wattzap.view.RouteFilePicker;
import com.wattzap.view.VideoPlayer;
import com.wattzap.view.prefs.Preferences;
import com.wattzap.view.training.TrainingDisplay;
import com.wattzap.view.training.TrainingPicker;

/**
 * (c) 2013 David George / Wattzap.com
 *
 * @author David George
 * @date 11 June 2013
 */
public class Main implements Runnable {
	private static Logger logger = LogManager.getLogger("Main");
	private final static UserPreferences userPrefs = UserPreferences.INSTANCE;

	public static void main(String[] args) {
		// Debug
		Level level = setLogLevel();
		NativeLibrary.addSearchPath("libvlc", "C:/usr/vlc-2.0.6/");
		// configure the appender
		String PATTERN = "%r [%t] %p %c %x %m%n";
		String logFile = userPrefs.getWD() + "/logfile.txt";
		FileAppender fileAppender;
		try {
			fileAppender = new FileAppender(new PatternLayout(PATTERN), logFile);
			fileAppender.setThreshold(level);
			fileAppender.activateOptions();
			// add appender to any Logger (here is root)
			Logger.getRootLogger().addAppender(fileAppender);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // create appender

		// Turn on Debug window
		if (userPrefs.isDebug()) {
			SwingAppender appender = new SwingAppender(); // create appender
			// configure the appender

			appender.setLayout(new PatternLayout(PATTERN));
			appender.setThreshold(level);
			appender.activateOptions();
			// add appender to any Logger (here is root)
			Logger.getRootLogger().addAppender(appender);
		}

		logger.info("Setting log level => " + level.toString());

		logger.info("Database Version " + userPrefs.getDBVersion());
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}

			EventQueue.invokeLater(new Main());
		} catch (Exception e) {
			// catch everything and log
			logger.error(e.getLocalizedMessage());
			userPrefs.shutDown();
		}
	}

	@Override
	public void run() {
		MainFrame frame = new MainFrame();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		// frame.setSize(screenSize.width, screenSize.height-100);
		frame.setBounds(userPrefs.getMainBounds());

		// Must be declared above Odometer
		//AdvancedSpeedCadenceListener scListener = null;
		JPanel odo = null;
		try {
            new TelemetryProvider().initialize();
			new AntSubsystem().initialize();
            new SpeedAndCadenceSensor().initialize();
			odo = new AntOdometer();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frame, "ANT+ " + e.getMessage(),
					userPrefs.messages.getString("warning"),
					JOptionPane.WARNING_MESSAGE);
			logger.error("ANT+ " + e.getMessage());
			new DummySpeedCadenceListener();
			userPrefs.setAntEnabled(false);
			odo = new Odometer();
		}

		// Performs an isregister check, be careful if we move below AboutPanel
		VideoPlayer videoPlayer = new VideoPlayer(frame, odo);
		try {
			videoPlayer.init();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frame, e.getMessage(),
					userPrefs.messages.getString("warning"),
					JOptionPane.INFORMATION_MESSAGE);
			logger.info(e.getMessage());

			videoPlayer = null;
		}

		MigLayout layout = new MigLayout("center", "[]10px[]", "");
		Container contentPane = frame.getContentPane();
		contentPane.setBackground(Color.BLACK);
		contentPane.setLayout(layout);

		// create view
		new Map(frame);
		Profile profile = new Profile(screenSize);
		profile.setVisible(false);

		// Menu Bar
		JMenuBar menuBar = new JMenuBar();

		JMenu appMenu = new JMenu("Application");
		menuBar.add(appMenu);
		// Preferences
		Preferences preferences = new Preferences();
		JMenuItem prefMenuItem = new JMenuItem(
				userPrefs.messages.getString("preferences"));
		prefMenuItem.addActionListener(preferences);
		appMenu.add(prefMenuItem);

		JMenuItem aboutMenuItem = new JMenuItem(
				userPrefs.messages.getString("about"));
		appMenu.add(aboutMenuItem);
		// NOTE: Sets up timer for unregistered users.
		AboutPanel about = new AboutPanel();
		aboutMenuItem.addActionListener(about);

		JMenuItem quitMenuItem = new JMenuItem(
				userPrefs.messages.getString("quit"));
		appMenu.add(quitMenuItem);
		quitMenuItem.addActionListener(frame);
		quitMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask(), false));

		// Route
		JMenu fileMenu = new JMenu(userPrefs.messages.getString("route"));
		menuBar.add(fileMenu);
		JMenuItem openMenuItem = new JMenuItem(
				userPrefs.messages.getString("open"));
		fileMenu.add(openMenuItem);
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask(), false));

		RouteFilePicker picker = new RouteFilePicker(frame);
		openMenuItem.addActionListener(picker);

		MenuItem closeMenuItem = new MenuItem(Messages.CLOSE,
				userPrefs.messages.getString("close"));
		fileMenu.add(closeMenuItem);

		closeMenuItem.setAccelerator(KeyStroke.getKeyStroke('C', Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask(), false));

		// Submenu: Training
		JMenu trainingMenu = new JMenu(userPrefs.messages.getString("training"));
		menuBar.add(trainingMenu);
		TrainingDisplay trainingDisplay = new TrainingDisplay(screenSize);
		TrainingController trainingController = new TrainingController(
				trainingDisplay, frame);

		if (userPrefs.isAntEnabled()) {
			// Submenu: training
			JMenuItem trainMenuItem = new JMenuItem(
					userPrefs.messages.getString("open"));
			trainMenuItem.setActionCommand(TrainingController.open);
			trainingMenu.add(trainMenuItem);

			TrainingPicker tPicker = new TrainingPicker(frame);
			trainMenuItem.addActionListener(tPicker);
		}
		JMenu analizeMenuItem = new JMenu(
				userPrefs.messages.getString("analyze"));
		trainingMenu.add(analizeMenuItem);

		JMenuItem analMenuItem = new JMenuItem(
				userPrefs.messages.getString("analyze"));
		analMenuItem.setActionCommand(TrainingController.analyze);
		analizeMenuItem.add(analMenuItem);

		JMenuItem saveMenuItem = new JMenuItem(
				userPrefs.messages.getString("save"));
		saveMenuItem.setActionCommand(TrainingController.save);
		trainingMenu.add(saveMenuItem);

		JMenuItem viewMenuItem = new JMenuItem(
				userPrefs.messages.getString("view"));
		viewMenuItem.setActionCommand(TrainingController.view);
		trainingMenu.add(viewMenuItem);

		JMenuItem recoverMenuItem = new JMenuItem(
				userPrefs.messages.getString("recover"));
		recoverMenuItem.setActionCommand(TrainingController.recover);
		trainingMenu.add(recoverMenuItem);

		analMenuItem.addActionListener(trainingController);
		saveMenuItem.addActionListener(trainingController);
		recoverMenuItem.addActionListener(trainingController);

		viewMenuItem.addActionListener(trainingController);

		frame.add(trainingDisplay, "cell 0 0");

		frame.setJMenuBar(menuBar);
		// End Menu

		frame.add(profile, "cell 0 1, grow");
		// by default add to telemetry frame
		frame.add(odo, "cell 0 2, grow");

		ControlPanel cp = new ControlPanel();
		frame.add(cp, "cell 0 3");

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}

	private static Level setLogLevel() {
		final String LOGGER_PREFIX = "log4j.logger.";

		for (String propertyName : System.getProperties().stringPropertyNames()) {
			if (propertyName.startsWith(LOGGER_PREFIX)) {
				String loggerName = propertyName.substring(LOGGER_PREFIX
						.length());
				String levelName = System.getProperty(propertyName, "");
				Level level = Level.toLevel(levelName); // defaults to DEBUG
				if (!"".equals(levelName)
						&& !levelName.toUpperCase().equals(level.toString())) {
					logger.error("Skipping unrecognized log4j log level "
							+ levelName + ": -D" + propertyName + "="
							+ levelName);
					continue;
				}
				return level;

			}
		}
		return Level.ERROR;
	}
}
