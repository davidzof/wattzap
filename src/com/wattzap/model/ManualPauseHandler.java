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

import com.wattzap.model.dto.Telemetry;

/**
 * Handles manual pause (requests from menu or keyboard).
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class ManualPauseHandler extends TelemetryHandler {

    private boolean speedLost = false;

    @Override
    public String getPrettyName() {
        return "manualPause";
    }

    @Override
    public void configChanged(UserPreferences pref) {
        if (pref == UserPreferences.MANUAL_PAUSE) {
            speedLost = false;
            setPause(pref.isPaused() ?
                    PauseMsgEnum.PAUSED :
                    PauseMsgEnum.RUNNING);
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return (data == SourceDataEnum.PAUSE);
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // when paused and speed changes from not_available to something else
        // unpause. It is done via config changed
        switch (t.getValidity(SourceDataEnum.WHEEL_SPEED)) {
            case NOT_PRESENT:
                // wheel speed is not present.. Configuration was changed?
                break;
            case NOT_AVAILABLE:
                // speed is not available: no motion condition detected?
                speedLost = true;
                break;
            default:
                // speed is back, if paused unpause immediatelly
                if ((speedLost) && (UserPreferences.MANUAL_PAUSE.isPaused())) {
                    UserPreferences.MANUAL_PAUSE.setPaused(false);
                }
                break;
        }
    }
}
