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
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import java.awt.Color;

/**
 * Opponent data
 * @author Jarek
 */
public class Opponent {
    private static int serial = 1;

    private int id;
    private RouteReader reader;
    private String name;
    private Color color;
    // calculated on route
    private long time = -1;
    private double distance = 0.0;
    private double speed = 0.0;
    private double[] latLon = null;
    private PauseMsgEnum pause = PauseMsgEnum.WRONG_TRAINING;
    private int rank = 0;

    private final Telemetry t = new Telemetry(PauseMsgEnum.RUNNING);

    public Opponent(RouteReader reader) {
        id = (serial++);
        setReader(reader);
        name = "Ghost";
        color = Color.YELLOW;
    }

    public final void setReader(RouteReader rd) {
        if (rd != null) {
            if (reader != null) {
                reader.close();
            }
            reader = rd;
            if (reader != null) {
                reader.setPrettyName("opp" + id);
            }
        }
        // reinitialize reader
        distance = -1.0;
        time = -1;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public Color getColor() {
        return color;
    }

    // get "ghost" distance for "local" training time
    public double getDistance(long tt) {
        // the only interesting data is routeSpeed. On it distance is computed
        // I assume that video speed doesn't change too much between points
        // and points cover relatively long segments. Otherwise distance might
        // be calculated a bit wrong.
        if (reader == null) {
            pause = PauseMsgEnum.WRONG_TRAINING;
        } else if (!reader.provides(SourceDataEnum.ROUTE_SPEED)) {
            // show wrong training icon.. cannot show anything more..
            pause = PauseMsgEnum.WRONG_TRAINING;
            speed = 0.0;
        } else if (time < 0) {
            // store initial timeOffset
            // show question mark icon, nothing started yet
            pause = PauseMsgEnum.INITIALIZE;
            distance = 0.0;
            speed = 0.0;
            rank = 0;
        } else if (rank != 0) {
            // nothing.. opponent has finished the race
        } else {
            t.setDistance(distance);
            reader.storeTelemetryData(t);
            if (reader.provides(SourceDataEnum.LATITUDE) &&
                    reader.provides(SourceDataEnum.LONGITUDE))
            {
                if (latLon == null) {
                    latLon = new double[2];
                }
                latLon[0] = reader.getValue(SourceDataEnum.LATITUDE);
                latLon[1] = reader.getValue(SourceDataEnum.LONGITUDE);
            } else {
                latLon = null;
            }
            pause = PauseMsgEnum.get((int) reader.getValue(SourceDataEnum.PAUSE));
            if (pause.key() == null) {
                speed = reader.getValue(SourceDataEnum.ROUTE_SPEED);
                // distance [km], while speed [km/h], time [ms]
                distance += (speed / 3600.0) * ((double) (tt - time)) / 1000.0;
            } else {
                speed = 0;
            }
        }
        time = tt;
        return distance;
    }
    public PauseMsgEnum getPause() {
        return pause;
    }
    public double getSpeed() {
        return speed;
    }
    double[] getLatLon() {
        return latLon;
    }


    public int getRank() {
        return rank;
    }
    public void setRank(int rank) {
        this.rank = rank;
        switch (rank) {
            case 0:
                pause = PauseMsgEnum.INITIALIZE;
                time = -1;
                break;
            case 1:
                pause = PauseMsgEnum.FIRST_POSITION;
                break;
            case 2:
                pause = PauseMsgEnum.FIRST_POSITION;
                break;
            case 3:
                pause = PauseMsgEnum.FIRST_POSITION;
                break;
            default:
                pause = PauseMsgEnum.RACE_FINISHED;
                break;
        }
    }
}
