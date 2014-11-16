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
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.UserPreferences;
import javax.swing.JPanel;

/**
 * Handles data selectors, their configs and additional flags
 *
 * @author Jarek
 */
public class SourcesPanel extends ConfigPanel {
    private UserPreferences userPrefs = UserPreferences.INSTANCE;
    private Thread thread = null;
    private JPanel sensorPanel;

    public SourcesPanel() {
		super();

        add(new ConfigFieldCheck(this, UserPreferences.WHEEL_SPEED_VISIBLE, "ws_visible"));

        // selectors for speed/cadence/hr/power and additional params
        add(new ConfigFieldSourceSelector(this, UserPreferences.SPEED_SOURCE,
                "speed_source", SourceDataEnum.WHEEL_SPEED));
        add(new ConfigFieldDouble(this, UserPreferences.ROBOT_SPEED, "robot_speed",
                "%.1f", "km/h", "mph", Constants.KMTOMILES));

        add(new ConfigFieldSourceSelector(this, UserPreferences.CADENCE_SOURCE,
                "cadence_source", SourceDataEnum.CADENCE));

        add(new ConfigFieldSourceSelector(this, UserPreferences.HR_SOURCE,
                "hr_source", SourceDataEnum.HEART_RATE));

        add(new ConfigFieldSourceSelector(this, UserPreferences.POWER_SOURCE,
                "power_source", SourceDataEnum.POWER));
        add(new ConfigFieldInt(this, UserPreferences.ROBOT_POWER, "robot_power", "W"));
	}
}
