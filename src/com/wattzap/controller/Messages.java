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
    // data: ???
    CONFIG_CHANGED,

    // subsystem opened or closed. Reconfiguration is allowed only when stopped
    // so before changing configuration it must be stopped.
    // data: subsytem object
    SUBSYSTEM, SUBSYSTEM_REMOVED,

    // sensor handler started, reconfigured or stoped
    // data: handler object
    HANDLER, HANDLER_REMOVED,

    TELEMETRY,

    @Deprecated
    HEARTRATE,
    @Deprecated
    SPEEDCADENCE,

    // first and last message of the training? Save on stop?
    START, STOP,

    @Deprecated
    TRAININGITEM,
    @Deprecated
    TRAINING,

    WORKOUT_DATA,

    // position within the video
    // data: (double) position
    STARTPOS,

    // TODO replace with TRAINING
    GPXLOAD,
    CLOSE;
}
