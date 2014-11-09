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
import java.util.UUID;

import com.wattzap.model.dto.WorkoutData;
import com.wattzap.model.power.Power;
import com.wattzap.model.power.PowerProfiles;

/**
 * Singleton helper to read/write user preferences to a backing store
 *
 * @author David George / 15 September 2013 (C) Copyright 2013
 * @author Jarek
 */
public enum UserPreferences {
    TURBO_TRAINER("profile", "Tacx Satori / Blue Motion"),
    RESISTANCE("resistance", 1),
    RESISTANCE_COMP("resistance_comp", "same_speed"),

    SPEED_SOURCE("speed_source", "speed2power"),
    CADENCE_SOURCE("cadence_source", "sandc"),
    HR_SOURCE("hr_source", "hrm"),
    POWER_SOURCE("power_source", "sandc"),

    DEBUG("debug", false),
    MAX_POWER("maxpower", 250),
    HR_MAX("maxhr", 180),

    // robot power/speed.
    ROBOT_POWER("robot", 210),
    ROBOT_SPEED("robot_speed", 30.0, 0.1),

    // 2133 is 700Cx23
    WHEEL_SIZE("wheelsize", 2133),
    BIKE_WEIGHT("bikeweight", 10.0, 0.1),
    WEIGHT("weight", 75.0, 0.1),

    LANG("lang", "EN"),

    EVAL_TIME("evalTime", 240),
    SERIAL("ssn", ""),
    REGKEY("rsnn", ""),

    TRAININGS_DIR("trainingLocation", getWD() + "/Trainings"),
    VIDEO_DIR("videoLocation", getWD() + "/Routes"),

    RUNNING("running", false),
    AUTO_START("autostart", false),
    LAST_FILENAME("last_filename", ""),
    LOAD_LAST("load_last", false),


    ANT_ENABLED("ant_enabled", true),
    ANT_USBM("antusbm", false),

    METRIC("units", true),

    DB_VERSION("dbVersion", "1.2"),

    // special property for sensors, used for notification about sensorId change
    // for specified sensorName.
    SENSORS("sensors"),
    // special property, not handled by the database
    PAIRING("pairing", false),

    @Deprecated
    SIMUL_SPEED("simulSpeed", false),

    // backward compability, cannot be get/set
	INSTANCE;

    static {
        SERIAL.forAll = true;
        REGKEY.forAll = true;
        EVAL_TIME.forAll = true;
        EVAL_TIME.intCrypted = true;
        PAIRING.keptInDB = false;
    }

	// why it must be always specified?? Are there system settings
    // and user settings? Hmm. serial? evalTime? What else?
    private static String user = System.getProperty("user.name");

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
    private Boolean keptInDB = true;

    private UserPreferences() {
        this(null);
    }
    private UserPreferences(String name) {
        this.name = name;
        this.initialized = true;
        this.keptInDB = false;
    }
    private UserPreferences(String name, String val) {
        this.name = name;
        strVal = val;
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
    private UserPreferences(String name, boolean val) {
        this.name = name;
        boolVal = val;
    }


    public String getName() {
        return name;
    }
    public String getString() {
        synchronized(this) {
            assert (strVal != null) : name + " is not a string";
            if (!initialized) {
                if (keptInDB) {
                    strVal = get(forAll ? "" : user, name, strVal);
                }
                initialized = true;
            }
            return strVal;
        }
    }
    public void setString(String val) {
        synchronized(this) {
            if (!getString().equals(val)) {
                strVal = val;
                if (keptInDB) {
                    set(forAll ? "" : user, name, val);
                }
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
            }
        }
    }
    public int getInt() {
        synchronized(this) {
            assert (intVal != null) : name + " is not an integer";
            if (!initialized) {
                if (keptInDB) {
                    if (intCrypted) {
                        intVal = getIntCrypt(forAll ? "" : user, name, intVal);
                    } else {
                        intVal = getInt(forAll ? "" : user, name, intVal);
                    }
                }
                initialized = true;
            }
            return intVal;
        }
    }
    public void setInt(int val) {
        synchronized(this) {
            if (getInt()!= val) {
                intVal = val;
                if (keptInDB) {
                    if (intCrypted) {
                        setIntCrypt(forAll ? "" : user, name, val);
                    } else {
                        setInt(forAll ? "" : user, name, val);
                    }
                }
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
            }
        }
    }
    public double getDouble() {
        synchronized(this) {
            assert (doubleVal != null) : name + " is not a double";
            if (!initialized) {
                if (keptInDB) {
                    doubleVal = getDouble(forAll ? "" : user, name, doubleVal);
                }
                initialized = true;
            }
            return doubleVal;
        }
    }
    public void setDouble(double val) {
        synchronized(this) {
            double diff = getDouble() - val;
            if ((diff < -doubleDiff) || (diff > doubleDiff)) {
                doubleVal = val;
                if (keptInDB) {
                    setDouble(forAll ? "" : user, name, val);
                }
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
            }
        }
    }
    public boolean getBool() {
        synchronized(this) {
            assert (boolVal != null) : name + " is not a bool";
            if (!initialized) {
                if (keptInDB) {
                    boolVal = getBoolean(forAll ? "" : user, name, boolVal);
                }
                initialized = true;
            }
            return boolVal;
        }
    }
    public void setBool(boolean val) {
        synchronized(this) {
            if (getBool() != val) {
                boolVal = val;
                if (keptInDB) {
                    setBoolean(forAll ? "" : user, name, val);
                }
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, this);
            }
        }
    }






	// These functions are called only by GUI
    public Rectangle getMainBounds() {
		int width = getInt(user, "mainWidth", 1200);
		int height = getInt(user, "mainHeight", 650);
		int x = getInt(user, "mainX", 0);
		int y = getInt(user, "mainY", 0);
		Rectangle r = new Rectangle(x, y, width, height);
		return r;
	}
	public void setMainBounds(Rectangle r) {
		setInt(user, "mainHeight", r.height);
		setInt(user, "mainWidth", r.width);
		setInt(user, "mainX", r.x);
		setInt(user, "mainY", r.y);
	}
	public Rectangle getVideoBounds() {
		int width = getInt(user, "videoWidth", 800);
		int height = getInt(user, "videoHeight", 600);
		int x = getInt(user, "videoX", 0);
		int y = getInt(user, "videoY", 650);
		Rectangle r = new Rectangle(x, y, width, height);
		return r;
	}
	public void setVideoBounds(Rectangle r) {
		setInt(user, "videoHeight", r.height);
		setInt(user, "videoWidth", r.width);
		setInt(user, "videoX", r.x);
		setInt(user, "videoY", r.y);
	}



    public boolean isStarted() {
        return RUNNING.getBool();
    }
    public void setStarted(boolean started) {
        RUNNING.setBool(started);
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

    // TODO replace with FtHR
	public int getMaxHR() {
		return HR_MAX.getInt();
	}
	public void setMaxHR(int hr) {
		HR_MAX.setInt(hr);
	}

	public int getMaxPower() {
        return MAX_POWER.getInt();
	}
	public void setMaxPower(int power) {
		MAX_POWER.setInt(power);
	}

	public boolean isMetric() {
        return METRIC.getBool();
	}
	public void setMetric(boolean value) {
        METRIC.setBool(value);
	}

	public boolean isAntUSBM() {
        return ANT_USBM.getBool();
	}
	public void setAntUSBM(boolean value) {
		ANT_USBM.setBool(value);
	}

    public Locale getLocale() {
        return Locale.forLanguageTag(LANG.getString());
    }
    public String getLang() {
        return LANG.getString();
    }
    public void setLang(String lang) {
        LANG.setString(lang);
    }

	public boolean isDebug() {
        return DEBUG.getBool();
	}
	public void setDebug(boolean value) {
        DEBUG.setBool(value);
	}

    public int getRobotPower() {
        return ROBOT_POWER.getInt();
    }
    public void setRobotPower(int power) {
        ROBOT_POWER.setInt(power);
    }

    public Power getTurboTrainerProfile() {
        synchronized(TURBO_TRAINER) {
            String profile = TURBO_TRAINER.getString();
            return PowerProfiles.INSTANCE.getProfile(profile);
        }
	}
	public void setTurboTrainer(String profile) {
        TURBO_TRAINER.setString(profile);
	}

	public int getResistance() {
        return RESISTANCE.getInt();
	}
	public void setResistance(int r) {
        RESISTANCE.setInt(r);
	}

	public String getResistanceComp() {
        return RESISTANCE_COMP.getString();
	}
	public void setResistanceComp(String comp) {
        RESISTANCE_COMP.setString(comp);
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

    public String getDefaultFilename() {
        if ((!LOAD_LAST.getBool()) || (LAST_FILENAME.getString().isEmpty())) {
            return null;
        }
        return LAST_FILENAME.getString();
    }

    public boolean autostart() {
        return AUTO_START.getBool();
    }



    // Registration Stuff
	public String getSerial() {
        synchronized(SERIAL) {
            String serial = SERIAL.getString();
            if (serial.isEmpty()) {
                serial = UUID.randomUUID().toString();
                SERIAL.setString(serial);
            }
            return serial;
        }
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


    @Deprecated
    public boolean isVirtualPower() {
        return SIMUL_SPEED.getBool();
    }
    @Deprecated
    public void setVirtualPower(boolean ss) {
        SIMUL_SPEED.setBool(ss);
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

    public boolean isPairingEnabled() {
        return PAIRING.getBool();
    }
    public void setPairing(boolean enabled) {
        PAIRING.setBool(enabled);
    }

    public void setSensorId(String sensorName, int sensorId) {
        // sensor id might be changed from multiple threads at once:
        // the most "dangerous" one is SensorID query thread: multiple
        // sensors might report id more or less same time
        // This notification must be synchronized, or assertions are raised.
        synchronized(SENSORS) {
            if (getInt(user, "*" + sensorName, 0)  != sensorId) {
                setInt(user, "*" + sensorName, sensorId);
                SENSORS.strVal = sensorName;
                SENSORS.intVal = sensorId;
                MessageBus.INSTANCE.send(Messages.CONFIG_CHANGED, SENSORS);
                SENSORS.strVal = null;
                SENSORS.intVal = null;
            }
        }
    }
    public int getSensorId(String sensorName) {
        synchronized(SENSORS) {
            return getInt(user, "*" + sensorName, 0);
        }
	}
    public void removeSensor(String sensorName) {
        synchronized(SENSORS) {
            set(user, "*" + sensorName, null);
        }
    }




    public void addWorkout(WorkoutData data) {
		getDS().saveWorkOut(user, data);
	}
	public WorkoutData getWorkout(String name) {
		return getDS().getWorkout(user, name);
	}
	public void deleteWorkout(String name) {
		getDS().deleteWorkout(user, name);
	}
	public List<WorkoutData> listWorkouts() {
		return getDS().listWorkouts(user);
	}

	// Data Access Functions
	private static DataStore ds = null;
    private DataStore getDS() {
        if (ds == null) {
        	String cryptKey = "afghanistanbananastan";
            ds = new DataStore(getWD(), cryptKey);
        }
        return ds;
    }
	public void shutDown() {
        if (ds != null) {
    		ds.close();
            ds = null;
        }
	}

	private double getDouble(String user, String key, double d) {
		String v = getDS().getProp(user, key);
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

	private void setDouble(String user, String key, double d) {
		ds.insertProp(user, key, Double.toString(d));
	}

	private int getInt(String user, String key, int i) {
		String v = getDS().getProp(user, key);
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

	private int getIntCrypt(String user, String key, int i) {
		String v = getDS().getPropCrypt(user, key);
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

	private void setInt(String user, String key, int i) {
		ds.insertProp(user, key, Integer.toString(i));
	}

	private static void setIntCrypt(String user, String key, int i) {
		ds.insertPropCrypt(user, key, Integer.toString(i));
	}

	private boolean getBoolean(String user, String key, boolean b) {
		String v = getDS().getProp(user, key);
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

	private void setBoolean(String user, String key, boolean b) {
		ds.insertProp(user, key, Boolean.toString(b));
	}

	private String get(String user, String key, String s) {
		String v = getDS().getProp(user, key);
		if (v == null) {
			v = s;
		}
		return v;
	}

	private void set(String user, String key, String s) {
        if (s != null) {
    		ds.insertProp(user, key, s);
        } else {
            ds.deleteProp(user, key);
        }
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
