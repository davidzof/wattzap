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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLayeredPane;
import javax.swing.WindowConstants;

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
public class MainFrame extends JFrame
        implements ActionListener, MessageCallback
{
	private static final String appName = "WattzAp";
	private static final Logger logger = LogManager.getLogger("Main Frame");

    // shortcuts for XXX_VISIBLE preferences
    private static final UserPreferences train = UserPreferences.TRAINING_VISIBLE;
    private static final UserPreferences map = UserPreferences.MAP_VISIBLE;
    private static final UserPreferences prof = UserPreferences.PROFILE_VISIBLE;
    private static final UserPreferences odo = UserPreferences.ODO_VISIBLE;
    private static final UserPreferences pause = UserPreferences.PAUSE_VISIBLE;
    private static final UserPreferences info = UserPreferences.INFO_VISIBLE;

    private final JLayeredPane lpane;
    private final Map<UserPreferences, Component> panels = new HashMap<>();
    private final Map<UserPreferences, String> constraints = new HashMap<>();

	public MainFrame() {
		super();

        setTitle(appName);
		ImageIcon img = new ImageIcon("icons/turbo.jpg");
		setIconImage(img.getImage());

        Container contentPane = getContentPane();
		contentPane.setBackground(Color.black);
		contentPane.setLayout(new BorderLayout());

        // set size of the window
		setBounds(UserPreferences.INSTANCE.getMainBounds());

        // panel to show all "scalable" components and overlaping description..
        lpane = new JLayeredPane();
        lpane.setLayout(new PercentLayout());
        add(lpane, BorderLayout.CENTER);

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

    public void add(UserPreferences pref, Component panel) {
        assert !panels.containsKey(pref) : "Panel for " + pref + " already added";

        panels.put(pref, panel);
        if (pref.getInt() >= 0) {
            lpane.setLayer(panel, pref.getInt());
        }

        callback(Messages.CONFIG_CHANGED, pref);
    }

    private synchronized void place(UserPreferences pref, Container cont, String constr) {
        // if component not added yet
        if (!panels.containsKey(pref)) {
            return;
        }
        if (pref.getBool()) {
            if (constraints.containsKey(pref)) {
                if (constr ==  null) {
                    if (constraints.get(pref) == null) {
                        return;
                    }
                } else {
                    if (constr.equals(constraints.get(pref))) {
                        return;
                    }
                }
                cont.remove(panels.get(pref));
            }
            constraints.put(pref, constr);

            cont.add(panels.get(pref), constr);
            panels.get(pref).invalidate();
            panels.get(pref).validate();
        } else {
            if (constraints.containsKey(pref)) {
                constraints.remove(pref);

                cont.remove(panels.get(pref));
            }
        }
    }

    public void rebuildForm() {
        String whole = "1-99.5";
        String upperY = whole;
        String lowerY = whole;
        if ((train.getBool() || map.getBool()) && (prof.getBool())) {
            upperY = "1-60.5";
            lowerY = "61-99.5";
        }
        String leftX = whole;
        String rightX = whole;
        if (train.getBool() && map.getBool()) {
            leftX = "0.5-60.5";
            rightX = "61-99.5";
        }

        //setVisible(true);
        //revalidate();
        invalidate();
        validate();

        place(train, lpane, leftX + "/" + upperY);
        place(map, lpane, rightX + "/" + upperY);
        place(prof, lpane, whole + "/" + lowerY);
        place(info, lpane, "west+0.5/north+1");
        place(pause, lpane, "30-70/40-60");
        // ODO is under main pane, not on layeredPane
        place(odo, this, BorderLayout.SOUTH);
    }

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
        	case CONFIG_CHANGED:
                UserPreferences pref = (UserPreferences) o;
                if (panels.containsKey(pref)) {
                    panels.get(pref).setVisible(pref.getBool());
                    rebuildForm();
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
