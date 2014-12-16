/*
 * This file is part of Wattzap Community Edition.
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
package com.wattzap.model.dto;

import com.wattzap.model.PauseMsgEnum;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author Jarek
 */
public class OpponentData {
	private static final Color skyBlue = new Color(0, 154, 237);

    private static final Icon wrongTraining = new ImageIcon("icons/wrong.jpg");
    private static final Icon firstPosition = new ImageIcon("icons/1st.jpg");
    private static final Icon secondPosition = new ImageIcon("icons/2nd.jpg");
    private static final Icon thirdPosition = new ImageIcon("icons/3rd.jpg");


    private final int id; // opponent id
    private final String name; // label
    private final Color color; // label color
    private final TelemetryValidityEnum validity; // to color text
    private final double distance; // value to be shown as text
    private final double passed; // distance ridden by robot
    private final PauseMsgEnum pause; // provides icon
    private final boolean latLon; // position to be shown on the map
    private final double lat;
    private final double lon;

    public OpponentData(Opponent opponent,
            double distance, double riderDist, TelemetryValidityEnum validity) {
        this.id = opponent.getId();
        this.name = opponent.getName();
        this.color = opponent.getColor();
        this.pause = opponent.getPause();
        double[] ll = opponent.getLatLon();
        if (ll != null) {
            this.latLon = true;
            this.lat = ll[0];
            this.lon = ll[1];
        } else {
            this.latLon = false;
            this.lat = 0.0;
            this.lon = 0.0;
        }
        this.distance = distance - riderDist;
        this.passed = distance;
        this.validity = validity;
    }


    public int getId() {
        return id;
    }
    public String getLabel()  {
        return name;
    }
    public Color getLabelColor() {
        return color;
    }
    public double getPassed() {
        return passed;
    }
    public Icon getIcon() {
        // TODO when multi-opponent panel is ready
        return null;
    }
    public double getDistance() {
        return distance;
    }
    public Color getDistanceColor() {
        if (pause.key() == null) {
            switch (validity) {
                case OK:
                    return Color.WHITE;
                case TOO_BIG:
                    return skyBlue;
                case TOO_SMALL:
                    return Color.RED;
            }
        }
        return Color.GRAY;
    }
    public boolean isLatLon() {
        return latLon;
    }
    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
}
