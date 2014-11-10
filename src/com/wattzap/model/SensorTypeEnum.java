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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enum used for creating all sensors defined in the configuration. For each
 * sensor is there a value type:sensorId which is read from config file for
 * entries *name.
 * Still need a lot of work, still speed&cadence and hrm are hardcoded.
 *
 * @author Jarek
 */
public enum SensorTypeEnum {
    // ANT+ sensors
    ANT_SPEED_CADENCE("ant_sc", SpeedAndCadenceSensor.class, SourceDataEnum.WHEEL_SPEED),
    ANT_HEART_RATE("ant_hr", HeartRateSensor.class, SourceDataEnum.HEART_RATE);

    private static final UserPreferences prefs = UserPreferences.SENSORS;

    // mapping from type (as a string)
    private static final Map<String, SensorTypeEnum> sensorTypes = new HashMap<>();
    static {
        for (SensorTypeEnum en : values()) {
            sensorTypes.put(en.getType(), en);
        }
    }
    public static SensorTypeEnum getByType(String type) {
        return sensorTypes.get(type);
    }

    // mapping from sensor class
    private static final Map<Class, SensorTypeEnum> sensorClasses = new HashMap<>();
    static {
        for (SensorTypeEnum en : values()) {
            sensorClasses.put(en.getSensorClass(), en);
        }
    }
    public static SensorTypeEnum getByClass(Class clazz) {
        return sensorClasses.get(clazz);
    }


    private final String type;
    private final Class clazz;
    private final SourceDataEnum defData;
    private SensorTypeEnum(String type, Class clazz, SourceDataEnum defData) {
        this.type = type;
        this.clazz = clazz;
        this.defData = defData;
    }
    public String getType() {
        return type;
    }
    public Class getSensorClass() {
        return clazz;
    }
    public SourceDataEnum getDefaultData() {
        return defData;
    }


    // TODO build it on config settings!
    public static void buildSensors() {
        List<String> sensors = prefs.getSensors();

        int found = 0;
        for (String sensor : sensors) {
            SensorTypeEnum type = prefs.getSensorType(sensor);
            if (type == null) {
                System.err.println("Sensor " + sensor + " has no type defined");
                prefs.removeSensor(sensor);
            } else {
                found++;
                buildSensor(sensor, type, prefs.getSensorId(sensor));
            }
        }
        // add default sensors if none defined. It takes place every time
        // all sensors were removed. To be removed!!!!
        if (found == 0) {
            System.err.println("Add default sensors..");
            prefs.setSensor("sandc", ANT_SPEED_CADENCE, 0);
            buildSensor("sandc", ANT_SPEED_CADENCE, 0);
            prefs.setSensor("hrm", ANT_HEART_RATE, 0);
            buildSensor("hrm", ANT_HEART_RATE, 0);
        }
    }

    public static void buildSensor(String name, SensorTypeEnum type, int id) {
        SensorIntf sensor = null;
        try {
            sensor = (SensorIntf) type.getSensorClass().newInstance();
        } catch (InstantiationException | IllegalAccessException ie) {
            assert false : "Cannot create " + name + " of type " + type;
        }
        if (sensor != null) {
            sensor.setPrettyName(name);
            sensor.initialize();
        }
    }
}
