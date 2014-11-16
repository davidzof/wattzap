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

import com.wattzap.model.ant.CadenceSensor;
import com.wattzap.model.ant.HeartRateSensor;
import com.wattzap.model.ant.SpeedAndCadenceSensor;
import com.wattzap.model.ant.SpeedSensor;
import com.wattzap.model.ant.StrideSensor;
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
public enum SensorTypeEnum implements EnumerationIntf {
    // ANT+ sensors
    ANT_SPEED_CADENCE("ant_sc", SpeedAndCadenceSensor.class, SourceDataEnum.WHEEL_SPEED),
    ANT_HEART_RATE("ant_hr", HeartRateSensor.class, SourceDataEnum.HEART_RATE),
    ANT_SPEED("ant_speed", SpeedSensor.class, SourceDataEnum.WHEEL_SPEED),
    ANT_CADENCE("ant_cad", CadenceSensor.class, SourceDataEnum.CADENCE),
    ANT_STRIDE_CAD("ant_stride", StrideSensor.class, SourceDataEnum.HEART_RATE);

    private static final UserPreferences prefs = UserPreferences.SENSORS;

    // mapping from type (as a string)
    private static final Map<String, SensorTypeEnum> sensorTypes = new HashMap<>();
    static {
        for (SensorTypeEnum en : values()) {
            sensorTypes.put(en.getKey(), en);
        }
    }
    public static SensorTypeEnum byType(String type) {
        return sensorTypes.get(type);
    }

    // mapping from sensor class
    private static final Map<Class, SensorTypeEnum> sensorClasses = new HashMap<>();
    static {
        for (SensorTypeEnum en : values()) {
            sensorClasses.put(en.getSensorClass(), en);
        }
    }
    public static SensorTypeEnum byClass(Class clazz) {
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

    @Override
    public String getKey() {
        return type;
    }
    public Class getSensorClass() {
        return clazz;
    }
    public SourceDataEnum getDefaultData() {
        return defData;
    }


    public static void buildSensors() {
        List<String> sensors = prefs.getSensors();
        for (String sensor : sensors) {
            buildSensor(sensor);
        }
    }

    public static SensorIntf buildSensor(String name) {
        SensorTypeEnum type = prefs.getSensorType(name);
        if (type == null) {
            System.err.println("Sensor " + name + " has unknown def " +
                    prefs.getSensor(name) + ", build HR");
            type = ANT_HEART_RATE;
        }
        return buildSensor(name, type, prefs.getSensorId(name));
    }
    public static SensorIntf buildSensor(String name, SensorTypeEnum type, int id) {
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
        return sensor;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean inBundle() {
        return true;
    }
}
