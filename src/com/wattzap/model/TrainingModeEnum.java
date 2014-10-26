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
package com.wattzap.model;

/**
 * Handled modes of training.
 * All available data is delivered in telemetry, so these values define only
 * general behaviour of some components (map, profile, video, odo).
 *
 * @author Jarek
 */
public enum TrainingModeEnum {
    /**
     * Training with time.
     * Route has only length as time. Speed and distance are not available.
     * On the route values like power, cadence, HR are only checked. Video runs
     * always with ratio 1:1. On ODO current (video) time is shown. Profile
     * shows only power profile of the training (and FTP is shown on the chart).
     * Map is not shown.
     * This mode is intended to be used by TRN files and when no trainig is
     * opened ("free run" with power)
     */
    TIME,

    /**
     * Training with distance.
     * Route has length [km]. Speed and distance are always available. Slope,
     * altitude, position might be available. Video runs with speed depending
     * on current speed (and route speed). Profile panel shows route profile
     * in the means of altitude. If altitude is not available, panel is hidden.
     * If positions are defined, route is shown on the map, white cross indicates
     * current position.
     * This mode is intended to be used with all route files (GPX, RLV, TCX).
     */
    DISTANCE;
}
