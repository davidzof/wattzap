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

import javax.swing.JOptionPane;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.gpxcreator.gpxpanel.GPXFile;
import com.gpxcreator.gpxpanel.GPXPanel;
import com.wattzap.MsgBundle;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import org.openstreetmap.gui.jmapviewer.OsmMercator;

/*
 * Displays a map of the course and moves cross-hairs depending on position.
 *
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 *
 * Removed "auto-scale" (quite annoying to track the route), added showing
 * whole route before start (hmm. map size is assumed 300x300)
 * @author Jarek
 */
public class Map extends GPXPanel implements MessageCallback {
	private static Logger logger = LogManager.getLogger("Map");

    private MainFrame frame;
	private GPXFile gpxFile = null;
    private int currentZoom;

	public Map(MainFrame frame) {
		super();

		// Alternative Source
		// check to see if tiles exist and use offline
		// see:
		// http://paulusschoutsen.nl/blog/2012/08/java-component-jmapviewer-with-offline-openstreetmap-support/
		// http://switch2osm.org/serving-tiles/
		// http://wiki.openstreetmap.org/wiki/JTileDownloader#Screenshots
		// this.setTileSource(tileSource)

		this.frame = frame;
		setVisible(false);

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
	}

	@Override
	public void callback(Messages message, Object o) {

		switch (message) {
		case TELEMETRY:
			Telemetry t = (Telemetry) o;
            // don't show position if not present!
            if ((!t.isAvailable(SourceDataEnum.LATITUDE)) ||
                    (!t.isAvailable(SourceDataEnum.LONGITUDE))) {
                return;
            }

			setCrosshairLat(t.getLatitude());
			setCrosshairLon(t.getLongitude());
            if (currentZoom < 0) {
                currentZoom = 13;
            } else {
    			currentZoom = getZoom();
            }
			setDisplayPositionByLatLon(t.getLatitude(), t.getLongitude(), currentZoom);
			setShowCrosshair(true);
			repaint();
			break;

        case CLOSE:
			if (this.isVisible()) {
				frame.remove(this);
				if (gpxFile != null) {
					this.removeGPXFile(gpxFile);
                    gpxFile = null;
				}
				setVisible(false);
				frame.invalidate();
				frame.validate();
				// frame.revalidate(); JDK 1.7 ONLY
			}
			break;

        case GPXLOAD:
			// code to see if we are registered
			if (!UserPreferences.INSTANCE.isRegistered()
					&& (UserPreferences.INSTANCE.getEvalTime()) <= 0) {
				logger.info("Out of time "
						+ UserPreferences.INSTANCE.getEvalTime());
				JOptionPane.showMessageDialog(this,
						MsgBundle.getString("trial_expired"),
						MsgBundle.getString("warning"),
                        JOptionPane.WARNING_MESSAGE);
				UserPreferences.INSTANCE.shutDown();
				System.exit(0);
			}

            if (isVisible()) {
				setVisible(false);
    			frame.remove(this);
				if (gpxFile != null) {
					this.removeGPXFile(gpxFile);
                    gpxFile = null;
				}
            }

            RouteReader routeData = (RouteReader) o;
			gpxFile = routeData.getGpxFile();
			if (gpxFile != null) {
                // How to get map size when app is started?? Lets assume
                // size 300x300
                int ww = getWidth();
                if (ww < 1) {
                    ww = 300;
                }
                int hh = getHeight();
                if (hh < 1) {
                    hh = 300;
                }
                for (currentZoom = 16; currentZoom > 3; currentZoom--) {
                    int wd = OsmMercator.LonToX(gpxFile.getMaxLon(), currentZoom) -
                            OsmMercator.LonToX(gpxFile.getMinLon(), currentZoom);
                    int ht = OsmMercator.LatToY(gpxFile.getMinLat(), currentZoom) -
                            OsmMercator.LatToY(gpxFile.getMaxLat(), currentZoom);
                    if (wd < 0) {
                        wd = -wd;
                    }
                    if (ht < 0) {
                        ht = -ht;
                    }
                    if ((wd <= ww) && (ht <= hh)) {
                        break;
                    }
                }
                double centerLon = (gpxFile.getMinLon() + gpxFile.getMaxLon()) / 2.0;
				double centerLat = (gpxFile.getMinLat() + gpxFile.getMaxLat()) / 2.0;
                setDisplayPositionByLatLon(centerLat, centerLon, currentZoom);

				this.addGPXFile(gpxFile);
    			repaint();

                frame.add(this, "cell 0 0");
                setVisible(true);

                // set "street view" zoom when started
                currentZoom = -1;
			}
            break;
		}
	}
}
