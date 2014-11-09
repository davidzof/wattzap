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
import com.wattzap.model.power.Power;

/**
 * Default profile: power bases on wheelSpeed and trainer resistance level.
 * Module is auto-loaded by SelectableDataSource, in fact trainers don't use
 * selection mechanism (yet).
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class ResistanceHandler extends TelemetryHandler {
    private int resistance;
    private boolean autoResistance;
    private Power power = null;

    @Override
    public String getPrettyName() {
        return "resistance";
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.TURBO_TRAINER)) {
            power = prefs.getTurboTrainerProfile();
        }

        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.RESISTANCE)) {
            if (prefs.getResistance() == 0) {
                // for most trainers neutral return 3..
                // Some trainers have only 1 level, so it cannot be used
                // for that purpose
                // resistance = power.getNeutral();

                resistance = 1;
                autoResistance = true;
            } else {
                resistance = prefs.getResistance();
                autoResistance = false;
            }
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        // resistance level is only provided when turbo has more than single
        // resistance level. Otherwise it is 1.
        if (data == SourceDataEnum.RESISTANCE) {
            return (power != null) && (power.getResitanceLevels() > 1);
        }
        return false;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        if (!provides(SourceDataEnum.RESISTANCE)) {
            return;
        }

        // default resistance taken from preferences
        if (autoResistance) {
            // Best matching (this is wheelSpeed best matches speed) shall be selected
            setValue(SourceDataEnum.RESISTANCE, 1);
        } else {
            // set default resistance: from config
            setValue(SourceDataEnum.RESISTANCE, resistance);
        }
    }
}
