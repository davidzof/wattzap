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

import com.wattzap.model.ant.HeartRateSensor;
import com.wattzap.model.ant.SpeedAndCadenceSensor;
import com.wattzap.view.prefs.ConfigFieldSensor;
import com.wattzap.view.prefs.ConfigPanel;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum used for creating all sensors defined in the configuration. For each
 * sensor is there a value type:sensorId which is read from config file for
 * entries *name.
 * Still need a lot of work, still speed&cadence and hrm are hardcoded.
 *
 * @author Jarek
 */
public enum SensorBuilder {
    ANT_SPEED_CADENCE("ant_sc", SpeedAndCadenceSensor.class),
    ANT_HEART_RATE("ant_hr", HeartRateSensor.class);

    static Map<String, Class> sensorTypes = new HashMap<>();
    static {
        for (SensorBuilder en : values()) {
            sensorTypes.put(en.getType(), en.getSensorClass());
        }
    }

    private final String type;
    private final Class clazz;
    private SensorBuilder(String type, Class clazz) {
        this.type = type;
        this.clazz = clazz;
    }
    public String getType() {
        return type;
    }
    public Class getSensorClass() {
        return clazz;
    }
    public static Class getByType(String type) {
        return sensorTypes.get(type);
    }

    // TODO build it on config settings!
    public static void buildSensors() {
        new SpeedAndCadenceSensor("sandc").initialize();
        new HeartRateSensor("hrm").initialize();
    }

    // TODO create panels for all existing sensors
    public static void buildFields(ConfigPanel panel) {
        panel.add(new ConfigFieldSensor(panel, "sandc", SourceDataEnum.WHEEL_SPEED));
        panel.add(new ConfigFieldSensor(panel, "hrm", SourceDataEnum.HEART_RATE));
        // new panels shall be added here..
    }
}
