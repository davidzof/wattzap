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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.RouteReader;
import com.wattzap.model.UserPreferences;
import java.awt.Color;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Main Window, displays telemetry data and responds to close events
 *
 * @author David George
 * @date 31 July 2013
 *
 * Main frame controlls all panels. Config attributes (not stored in DB) are
 * used.
 * @author: Jarek
 */
public class MainFrame extends JFrame implements ActionListener,  MessageCallback, Runnable {
	private static final String appName = "WattzAp";
	private static final Logger logger = LogManager.getLogger("Main Frame");

    // shortcuts for XXX_VISIBLE preferences
    private static UserPreferences train = UserPreferences.TRAINING_VISIBLE;
    private static UserPreferences map = UserPreferences.MAP_VISIBLE;
    private static UserPreferences prof = UserPreferences.PROFILE_VISIBLE;
    private static UserPreferences odo = UserPreferences.ODO_VISIBLE;

    private final Map<UserPreferences, JPanel> panels = new HashMap<>();
    private boolean requested = false;

	public MainFrame() {
		super();

		setBounds(UserPreferences.INSTANCE.getMainBounds());

        // build main window layout
        MigLayout layout = new MigLayout("center", "[fill, grow 100]10px[fill, grow 50]", "");
        Container contentPane = getContentPane();
		contentPane.setBackground(Color.black);
		contentPane.setLayout(layout);

        setTitle(appName);
		ImageIcon img = new ImageIcon("icons/turbo.jpg");
		setIconImage(img.getImage());

		MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);

        // show main frame on the screen, hide SplashScreen (where it is?)
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeApp();
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
        closeApp();
	}

    public void closeApp() {
        MessageBus.INSTANCE.send(Messages.EXIT_APP, null);

        // remember position and size
        Rectangle r = getBounds();
        UserPreferences.INSTANCE.setMainBounds(r);

        // shutdown database
        UserPreferences.INSTANCE.shutDown();
		System.exit(0);
    }

    public void add(UserPreferences pref, JPanel panel) {
        assert !panels.containsKey(pref) : "Panel for " + pref + " already added";

        panels.put(pref, panel);
        callback(Messages.CONFIG_CHANGED, pref);
    }

    public void rebuildForm() {
        // Remove all panels, if panel was not added, request is ignored
        for (JPanel panel : panels.values()) {
            remove(panel);
        }

        // show all fields.. any kind of "smart" code is to be build..
        if (map.getBool()) {
            if (train.getBool()) {
                add(panels.get(train));
                add(panels.get(map), "wrap");
            } else {
                add(panels.get(map), "span 2, wrap");
            }
        } else {
            add(panels.get(train), "span 2, wrap");
        }
        if (prof.getBool()) {
            add(panels.get(prof), "span 2, wrap");
        }
        if (odo.getBool()) {
            add(panels.get(odo), "span 2, wrap");
        }

        for (UserPreferences pref : panels.keySet()) {
            if (pref.getBool()) {
                panels.get(pref).invalidate();
                panels.get(pref).validate();
            }
        }
        //revalidate();
        setVisible(true);
        invalidate();
        validate();
    }

    public void run() {
        synchronized(this) {
            requested = false;
        }
        rebuildForm();
    }

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
        	case CONFIG_CHANGED:
                UserPreferences pref = (UserPreferences) o;
                if (panels.containsKey(pref)) {
                    panels.get(pref).setVisible(pref.getBool());
                    synchronized(this) {
                        if (!requested) {
                            requested = true;
                            SwingUtilities.invokeLater(this);
                        }
                    }
                }
                break;

            case GPXLOAD:
                RouteReader routeData = (RouteReader) o;
                String routeName = routeData.getName();
                if (routeName == null) {
                    routeName = appName;
                }
                setTitle(routeName);
                break;
            case CLOSE:
                setTitle(appName);
                break;
        }
    }
}
