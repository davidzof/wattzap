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

import com.wattzap.model.UserPreferences;

// TODO: Add video directory location
public class UserPanel extends ConfigPanel {
    private static final double LBSTOKG = 0.45359237;
    private UserPreferences userPrefs = UserPreferences.INSTANCE;


	public UserPanel() {
        super();

        add(new ConfigFieldDouble(this, UserPreferences.WEIGHT, "your_weight",
                "%.1f", "kg", "lbs", LBSTOKG) {
            @Override
            public double getProperty() {
                return userPrefs.getWeight();
            }
            @Override
            public void setProperty(double val) {
                userPrefs.setWeight(val);
            }
        });

        add(new ConfigFieldDouble(this, UserPreferences.BIKE_WEIGHT, "bike_weight",
                "%.1f", "kg", "lbs", LBSTOKG) {
            @Override
            public double getProperty() {
                return userPrefs.getBikeWeight();
            }
            @Override
            public void setProperty(double val) {
                userPrefs.setBikeWeight(val);
            }
        });

        add(new ConfigFieldInt(this, UserPreferences.WHEEL_SIZE, "wheel_size") {
            @Override
            public int getProperty() {
                return userPrefs.getWheelsize();
            }
            @Override
            public void setProperty(int val) {
                userPrefs.setWheelsize(val);
            }
        });

        add(new ConfigFieldInt(this, UserPreferences.HR_MAX, "fthr") {
            @Override
            public int getProperty() {
                return userPrefs.getMaxHR();
            }
            @Override
            public void setProperty(int val) {
                userPrefs.setMaxHR(val);
            }
        });

        add(new ConfigFieldInt(this, UserPreferences.MAX_POWER, "ftp") {
            @Override
            public int getProperty() {
                return userPrefs.getMaxPower();
            }
            @Override
            public void setProperty(int val) {
                userPrefs.setMaxPower(val);
            }
        });

        add(new ConfigFieldCheck(this, UserPreferences.METRIC, "metric") {
            @Override
            public boolean getProperty() {
                return userPrefs.isMetric();
            }
            @Override
            public void setProperty(boolean val) {
                userPrefs.setMetric(val);
            }
        });
	}
}