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
package com.wattzap.controller;

/**
 * (c) 2013 David George / Wattzap.com
 *
 * @author David George
 * @date 12 November 2013
 * @author Jarek
 */
public enum Messages {
    // any value in the config properties was changed (simple value, sensor,
    // subsystem, default source, trainer, etc).
    // data: changed property
// any value in the config properties was changed (simple value, sensor,
    // subsystem, default source, trainer, etc).
    // data: changed property
    CONFIG_CHANGED,

    // subsystem opened or closed. Reconfiguration is allowed only when stopped
    // so before changing configuration it must be stopped.
    // data: subsytem object
    SUBSYSTEM, SUBSYSTEM_REMOVED,

    // sensor handler started, reconfigured or stoped
    // data: handler object
    HANDLER, HANDLER_REMOVED,

    // complete data created by TelemetryProvider
    // data: Telemetry
    TELEMETRY,

    // start/stop training. These deal with subsystems/sensors, control
    // TelemetryProvider.
    // data: none
    START,
    STOP,

    // new training data was saved
    // data: WorkoutData with all computed parameters
    WORKOUT_DATA,

    // position within the video
    // data: (double) position
    STARTPOS,

    // message to be shown in the interface (somehow)
    // data: String
    ROUTE_MSG,

    // new profile is to be shown
    // data: XYSeries to be shown
    PROFILE,

    // Training data (routeReader) is loaded/closed.
    // data: routeReader
    // TODO replace with TRAINING
    GPXLOAD,
    CLOSE,

    // Application is going to be closed, modules shall handle their
    // own jobs.
    // data: nothing
    EXIT_APP,

    // training data (all telemetries recorded in the session)
    // data: training data collection
    TD,
    // request to "resend" training data
    // data: handler to get the TD notification
    TD_REQ;
}
