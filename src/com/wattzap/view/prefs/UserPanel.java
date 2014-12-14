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
package com.wattzap.view.prefs;

import com.wattzap.model.Constants;
import com.wattzap.model.UserPreferences;

// TODO: Add video directory location
public class UserPanel extends ConfigPanel {

	public UserPanel() {
        super();

        add(new ConfigFieldDouble(this, UserPreferences.WEIGHT, "your_weight",
                "%.1f", "kg", "lbs", Constants.LBSTOKG));
        add(new ConfigFieldDouble(this, UserPreferences.BIKE_WEIGHT, "bike_weight",
                "%.1f", "kg", "lbs", Constants.LBSTOKG));

        add(new ConfigFieldInt(this, UserPreferences.WHEEL_SIZE, "wheel_size"));
        add(new ConfigFieldInt(this, UserPreferences.HR_MAX, "fthr"));
        add(new ConfigFieldInt(this, UserPreferences.MAX_POWER, "ftp"));

        add(new ConfigFieldCheck(this, UserPreferences.METRIC, "metric"));

        add(new ConfigFieldCheck(this, UserPreferences.LOAD_LAST, "load_last"));
        add(new ConfigFieldCheck(this, UserPreferences.AUTO_START, "autostart"));
        add(new ConfigFieldCheck(this, UserPreferences.AUTO_SAVE, "autosave"));
	}
}