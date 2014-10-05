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
package com.wattzap.model;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import java.awt.Rectangle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

import com.wattzap.model.dto.WorkoutData;
import com.wattzap.model.power.Power;
import com.wattzap.model.power.PowerProfiles;

/**
 * Singleton helper to read/write user preferences to a backing store
 *
 * @author David George / 15 September 2013 (C) Copyright 2013
 *
 */
public enum UserPreferences {
    RESISTANCE("resistance", 1),
    VIRTUAL_POWER("virtualPower", 0),
    DEBUG("debug", false),
    MAX_POWER("maxpower", 250),
    HR_MAX("maxhr", 180),

    // 2133 is 700Cx23
    WHEEL_SIZE("wheelsize", 2133),
    BIKE_WEIGHT("bikeweight", 10.0, 0.1),
    WEIGHT("weight", 75.0, 0.1),

    LANG("lang", "EN"),

    EVAL_TIME("evalTime", 240, true),
    SERIAL("ssn", ""),
    REGKEY("rsnn", ""),

    TRAININGS_DIR("trainingLocation", getWD() + "/Trainings"),
    VIDEO_DIR("videoLocation", getWD() + "/Routes"),

    ANT_ENABLED("ant_enabled", true),
    ANT_USBM("antusbm", false),

    METRIC("units", true),
	POWER_PROFILE("profile", "Tacx Satori / Blue Motion"),

    DB_VERSION("dbVersion", "1.2"),

    // backward compability, cannot be get/set
	INSTANCE;

    static {
        SERIAL.forAll = true;
        REGKEY.forAll = true;
        EVAL_TIME.forAll = true;
    }

	// why it must be always specified?? Are there system settings
    // and user settings? Hmm. serial? evalTime? What else?
    private static String user = System.getProperty("user.name");

    // TODO with the use of configuration property..
	public static ResourceBundle messages;
    static {
        Locale currentLocale;
        currentLocale = Locale.getDefault();
		currentLocale = Locale.ENGLISH;
		messages = ResourceBundle.getBundle("MessageBundle", currentLocale);
    }

    // property values
    private final String name;
    private boolean forAll = false;
    private boolean initialized = false;
    private Double doubleVal = null;
    private double doubleDiff = 0.0;
    private Integer intVal = null;
    private boolean intCrypted = false;
    private String strVal = null;
    private Boolean boolVal = null;

    private UserPreferences() {
        this.name = null;
	}
    private UserPreferences(String name, String val) {
        this.name = name;
        strVal = val;
    }
    private UserPreferences(String name) {
        this.name = name;
        strVal = "";
    }
    private UserPreferences(String name, double val, double diff) {
        this.name = name;
        doubleVal = val;
        doubleDiff = diff;
    }
    private UserPreferences(String name, int val) {
        this.name = name;
        intVal = val;
    }
    private UserPreferences(String name, int val, boolean crypt) {
        this.name = name;
        intVal = val;
        intCrypted = crypt;
    }
    private UserPreferences(String name, boolean val) {
        this.name = name;
        boolVal = val;
    }


    public String getName() {
        return name;
    }
    public String getString() {
        if (strVal == null) {
            throw new UnsupportedOperationException(name + " is not a string");
        }
        if (!initialized) {
            strVal = get(forAll ? "" : user, name, strVal);
            initialized = true;
        }
        return strVal;
    }
    public void setString(String val) {
        if (!getString().equals(val)) {
            set(forAll ? "" : user, name, val);
            MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
        }
    }
    public int getInt() {
        if (intVal == null) {
            throw new UnsupportedOperationException(name + " is not a integer");
        }
        if (!initialized) {
            if (intCrypted) {
                intVal = getIntCrypt(forAll ? "" : user, name, intVal);
            } else {
                intVal = getInt(forAll ? "" : user, name, intVal);
            }
            initialized = true;
        }
        return intVal;
    }
    public void setInt(int val) {
        if (getInt()!= val) {
            if (intCrypted) {
                setIntCrypt(forAll ? "" : user, name, val);
            } else {
                setInt(forAll ? "" : user, name, val);
            }
            MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
        }
    }
    public double getDouble() {
        if (doubleVal == null) {
            throw new UnsupportedOperationException(name + " is not a double");
        }
        if (!initialized) {
            doubleVal = getDouble(forAll ? "" : user, name, doubleVal);
            initialized = true;
        }
        return doubleVal;
    }
    public void setDouble(double val) {
        double diff = getDouble() - val;
        if ((diff < -doubleDiff) || (diff > doubleDiff)) {
            setDouble(forAll ? "" : user, name, val);
            MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
        }
    }
    public boolean getBool() {
        if (boolVal == null) {
            throw new UnsupportedOperationException(name + " is not a bool");
        }
        if (!initialized) {
            boolVal = getBoolean(forAll ? "" : user, name, boolVal);
            initialized = true;
        }
        return boolVal;
    }
    public void setBool(boolean val) {
        if (getBool() != val) {
            setBoolean(forAll ? "" : user, name, val);
            MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
        }
    }






	public Rectangle getMainBounds() {
		int width = getInt("", "mainWidth", 1200);
		int height = getInt("", "mainHeight", 650);
		int x = getInt("", "mainX", 0);
		int y = getInt("", "mainY", 0);
		Rectangle r = new Rectangle(x, y, width, height);
		return r;
	}
	public void setMainBounds(Rectangle r) {
		setInt("", "mainHeight", r.height);
		setInt("", "mainWidth", r.width);
		setInt("", "mainX", r.x);
		setInt("", "mainY", r.y);
	}
	public Rectangle getVideoBounds() {
		int width = getInt("", "videoWidth", 800);
		int height = getInt("", "videoHeight", 600);
		int x = getInt("", "videoX", 0);
		int y = getInt("", "videoY", 650);
		Rectangle r = new Rectangle(x, y, width, height);
		return r;
	}
	public void setVideoBounds(Rectangle r) {
		setInt("", "videoHeight", r.height);
		setInt("", "videoWidth", r.width);
		setInt("", "videoX", r.x);
		setInt("", "videoY", r.y);
	}



    public boolean isAntEnabled() {
		return ANT_ENABLED.getBool();
	}
	public void setAntEnabled(boolean v) {
		ANT_ENABLED.setBool(v);
	}

	public String getDBVersion() {
        return DB_VERSION.getString();
	}
	public void setDBVersion(String v) {
        DB_VERSION.setString(v);
	}

	public double getWeight() {
		return WEIGHT.getDouble();
	}
	public void setWeight(double weight) {
		WEIGHT.setDouble(weight);
	}
	public double getBikeWeight() {
		return BIKE_WEIGHT.getDouble();
	}
	public void setBikeWeight(double weight) {
        BIKE_WEIGHT.setDouble(weight);
	}
	public double getTotalWeight() {
		return getWeight() + getBikeWeight();
	}

	public int getWheelsize() {
		return WHEEL_SIZE.getInt();
	}
	public double getWheelSizeCM() {
		return getWheelsize() / 10.0;
	}
	public void setWheelsize(int wheelsize) {
		WHEEL_SIZE.setInt(wheelsize);
	}

	public int getMaxHR() {
		return HR_MAX.getInt();
	}
	public void setMaxHR(int maxhr) {
		HR_MAX.setInt(maxhr);
	}

	public int getMaxPower() {
        return MAX_POWER.getInt();
	}
	public void setMaxPower(int maxPower) {
		MAX_POWER.setInt(maxPower);
	}

	public boolean isMetric() {
        return METRIC.getBool();
	}
	public void setMetric(boolean value) {
        METRIC.setBool(value);
	}

	public boolean isANTUSB() {
        return ANT_USBM.getBool();
	}
	public void setAntUSBM(boolean value) {
		ANT_USBM.setBool(value);
	}

	public boolean isDebug() {
        return DEBUG.getBool();
	}
	public void setDebug(boolean value) {
        DEBUG.setBool(value);
	}

	public VirtualPowerEnum getVirtualPower() {
        int vp = VIRTUAL_POWER.getInt();
        if ((vp < 0) || (vp >= VirtualPowerEnum.values().length)) {
            vp = 0;
            VIRTUAL_POWER.setInt(vp);
        }
		return VirtualPowerEnum.values()[vp];
	}
	public void setVirtualPower(VirtualPowerEnum value) {
        VIRTUAL_POWER.setInt(value.ordinal());
	}

    private Power powerProfile = null;
	public Power getPowerProfile() {
		if (powerProfile == null) {
    		String profile = POWER_PROFILE.getString();
			powerProfile = PowerProfiles.INSTANCE.getProfile(profile);
		}
		return powerProfile;
	}
	public void setPowerProfile(String profile) {
        POWER_PROFILE.setString(profile);
        powerProfile = PowerProfiles.INSTANCE.getProfile(profile);
	}

	public int getResistance() {
        return RESISTANCE.getInt();
	}
	public void setResistance(int r) {
        RESISTANCE.setInt(r);
	}

	public String getRouteDir() {
        return VIDEO_DIR.getString();
	}
	public void setRouteDir(String s) {
        VIDEO_DIR.setString(s);
	}

	public String getTrainingDir() {
        return TRAININGS_DIR.getString();
	}
	public void setTrainingDir(String s) {
        TRAININGS_DIR.setString(s);
	}

    // Registration Stuff
	public String getSerial() {
        String serial = SERIAL.getString();
        if (serial.isEmpty()) {
            serial = UUID.randomUUID().toString();
            SERIAL.setString(serial);
		}
		return serial;
	}

	public boolean isRegistered() {
        return !REGKEY.getString().isEmpty();
	}
	public String getRegistrationKey() {
        return REGKEY.getString();
	}
	public void setRegistrationKey(String key) {
		REGKEY.setString(key);
	}
	public int getEvalTime() {
        return EVAL_TIME.getInt();
	}
	public void setEvalTime(int t) {
		EVAL_TIME.setInt(t);
	}



    // sensors handling
	@Deprecated
    public int getSCId() {
		return getInt(user, "sandcId", 0);
	}

	@Deprecated
	public void setSCId(int i) {
		setInt(user, "sandcId", i);
	}

	@Deprecated
	public int getHRMId() {
		return getInt(user, "hrmid", 0);
	}

	@Deprecated
	public void setHRMId(int i) {
		setInt(user, "hrmid", i);
	}

    public void setSensorId(String sensorName, int sensorId) {
        setInt(user, sensorName + "id", sensorId);
    }
    public int getSensorId(String sensorName) {
		return getInt(user, sensorName + "id", 0);
	}
    public void removeSensor(String sensorName) {
        // TODO
    }




    public void addWorkout(WorkoutData data) {
		ds.saveWorkOut(user, data);
	}
	public WorkoutData getWorkout(String name) {
		return ds.getWorkout(user, name);
	}
	public void deleteWorkout(String name) {
		ds.deleteWorkout(user, name);
	}
	public List<WorkoutData> listWorkouts() {
		return ds.listWorkouts(user);
	}

    // TODO move to dataStore!
	// Data Access Functions
	private static final String cryptKey = "afghanistanbananastan";
	private static final DataStore ds = new DataStore(getWD(), cryptKey);

	private static double getDouble(String user, String key, double d) {
		String v = ds.getProp(user, key);
		if (v != null) {
			try {
				d = Double.parseDouble(v);
			} catch (Exception e) {
                // silently delete the property: it is not valid
                ds.deleteProp(user, key);
			}
		}
		return d;
	}

	private static void setDouble(String user, String key, double d) {
		ds.insertProp(user, key, Double.toString(d));
	}

	private static int getInt(String user, String key, int i) {
		String v = ds.getProp(user, key);
		if (v != null) {
			try {
				i = Integer.parseInt(v);
			} catch (Exception e) {
                // silently delete the property: it is not valid
                ds.deleteProp(user, key);
			}
		}
		return i;
	}

	private static int getIntCrypt(String user, String key, int i) {
		String v = ds.getPropCrypt(user, key);
		if (v != null) {
			try {
				i = Integer.parseInt(v);
			} catch (Exception e) {
                // silently delete the property: it is not valid
                ds.deleteProp(user, key);
			}
		}
		return i;
	}

	private static void setInt(String user, String key, int i) {
		ds.insertProp(user, key, Integer.toString(i));
	}

	private static void setIntCrypt(String user, String key, int i) {
		ds.insertPropCrypt(user, key, Integer.toString(i));
	}

	private static boolean getBoolean(String user, String key, boolean b) {
		String v = ds.getProp(user, key);
		if (v != null) {
			try {
				b = Boolean.parseBoolean(v);
			} catch (Exception e) {
                // silently delete the property: it is not valid
                ds.deleteProp(user, key);
			}
		}
		return b;
	}

	private static void setBoolean(String user, String key, boolean b) {
		ds.insertProp(user, key, Boolean.toString(b));
	}

	private static String get(String user, String key, String s) {
		String v = ds.getProp(user, key);
		if (v == null) {
			v = s;
		}
		return v;
	}

	private static void set(String user, String key, String s) {
		ds.insertProp(user, key, s);
	}

	public static void shutDown() {
		ds.close();
	}

	/*
	 * Stores common data files: database logfile Videos Trainings
	 *
	 * These directories are created by the Windows/Unix installer
	 *
	 * On Windows 7: C:\ProgramData\Wattzap On Windows XP: C:\Documents and
	 * Settings\All Users\Application Data\Wattzap On Unix: ??? $home/.wattzap
	 */
	private static String workingDirectory = null;
	public static String getWD() {
        if (workingDirectory == null) {
            // here, we assign the name of the OS, according to Java, to a
            // variable...
            String OS = (System.getProperty("os.name")).toUpperCase();
            // to determine what the workingDirectory is.
            // if it is some version of Windows

            if (OS.contains("WIN")) {
                // it is simply the location of the "AppData" folder
                workingDirectory = System.getenv("ALLUSERSPROFILE");

                if (OS.contains("WINDOWS XP")) {
                    workingDirectory += "/Application Data/Wattzap";
                } else {
                    workingDirectory += "/Wattzap";

                }
            } else {
                // in either case, we would start in the user's home directory
                workingDirectory = System.getProperty("user.home") + "/.wattzap";
            }
        }
		return workingDirectory;
	}

	/*
	 * Stores User dependent data Workouts
	 *
	 * On Windows 7: C:\Users\$user\AppData\Roaming\Wattzap On Windows XP:
	 * C:\Documents & Settings\$user\AppData\Wattzap On Unix: ??? $home/.wattzap
	 */
	private static String userDataDirectory = null;
	public static String getUserDataDirectory() {
		if (userDataDirectory == null) {
			// here, we assign the name of the OS, according to Java, to a
			// variable...
			String OS = (System.getProperty("os.name")).toUpperCase();

			// to determine what the workingDirectory is.
			// if it is some version of Windows
			if (OS.contains("WIN")) {
				// it is simply the location of the "AppData" folder
				userDataDirectory = System.getenv("APPDATA") + "/Wattzap";
			} else {
				// in either case, we would start in the user's home directory
				userDataDirectory = System.getProperty("user.home") + "/wattzap";
			}
		}
		return userDataDirectory;
	}
}
