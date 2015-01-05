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

import com.wattzap.PopupMessageIntf;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.utils.FileName;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wattzap.utils.ReflexiveClassLoader;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Load all available route readers, this lets us add Route Readers dynamically.
 *
 * @author David George
 * (c) 19 September 2014, David George / Wattzap.com
 */
public class Readers implements SourceDataHandlerIntf, MessageCallback {
	private static final Logger logger = LogManager.getLogger("Readers");

    private static Readers object = null;

    private static Readers getObject() {
        if (object == null) {
            object = new Readers();
            object.initialize();
        }
        return object;
    }


    public static FileNameExtensionFilter getExtensionFilter() {
		List<String> exts = new ArrayList<>();
		StringBuffer fileTypes = new StringBuffer();
        fileTypes.append("Supported file types (");
        String s = "";
		for (RouteReader reader : getObject().getReaders()) {
            if (reader.getExtension() != null) {
                fileTypes.append(s);
    			fileTypes.append(reader.getExtension());
                exts.add(reader.getExtension());
                s = ", ";
            }
		}
        fileTypes.append(")");

        return new FileNameExtensionFilter(
				fileTypes.toString(),
				exts.toArray(new String[exts.size()]));
    }

    public static RouteReader getTraining() {
        return getObject().getCurrentTraining();
    }

    public static String loadTraining(String fileName) {
        return getObject().load(fileName);
    }

    // Create new reader for given file. After use it shall be closed (to
    // garbage all the internal data).
    public static RouteReader createReader(String filename, PopupMessageIntf popup) {
        String ext = FileName.getExtension(filename);
		for (RouteReader r : getObject().getReaders()) {
			if (ext.equals(r.getExtension())) {
                try {
                    RouteReader reader = (RouteReader) r.getClass().newInstance();
                    // reader doesn't receive any configChanged callbacks. It
                    // only uses input file (read at start).
                    reader.configChanged(UserPreferences.INSTANCE);
                    reader.sendingMessages(false);
                    // readers do not have to be initialized.
                    String report = reader.load(filename);
                    if (report != null) {
                        logger.error("Cannot read " + filename + ":: " + report);
                        if (popup != null) {
                            popup.showWarning("Opponents", report);
                        }
                        return null;
                    }
                    return reader;
                } catch (InstantiationException | IllegalAccessException ex) {
                    // hm.. It is impossible, at least one reader already created
                    logger.error("Create instance, " + ex.getLocalizedMessage(), ex);
                    return null;
                }
			}
		}
        // in "normal" usage standard component shall be used, and the list of
        // of extensions is ok then. Otherwise.. warning in the log is ok
        logger.warn(filename + ":: extension '" + ext + "' not handled");
		return null;
    }




    private List<RouteReader> readers = null;

    private RouteReader currentTraining = null;
    private RouteReader dummyTraining = null;

    private RouteReader getCurrentTraining() {
        if (currentTraining != null) {
            return currentTraining;
        }
        if (dummyTraining == null) {
            dummyTraining = new DummyTraining();
        }
        return dummyTraining;
    }

	private List<RouteReader> getReaders() {
        if (readers == null) {
            readers = new ArrayList<RouteReader>();

            String packageName = getClass().getPackage().getName();
            getClassNamesFromPackage(packageName);
        }
		return readers;
	}

	private void getClassNamesFromPackage(String packageName) {
        List<Class> classes = null;
        try {
    		classes = ReflexiveClassLoader.getClassNamesFromPackage(
				packageName, RouteAnnotation.class);
        } catch (Exception e) {
            // IOException, URISyntaxException, ClassNotFoundException
            logger.error("Load classes, " + e.getLocalizedMessage(), e);
            return;
        }
		for (Class c : classes) {
            try {
    			RouteReader p = (RouteReader) c.newInstance();
                readers.add(p);
            } catch (Exception ex) {
                // InstantiationException, IllegalAccessException
                logger.error("Create instance, " + ex.getLocalizedMessage(), ex);
            }
		}
	}

	private RouteReader getReader(String ext) {
		for (RouteReader r : getReaders()) {
			if (ext.equals(r.getExtension())) {
				return r;
			}
		}
		return null;
	}

    public String load(String fileName) {
        String lastMessage = "Cannot start";

        if (currentTraining != null) {
            MessageBus.INSTANCE.send(Messages.CLOSE, currentTraining);
        }
        // current trainig = null;

        String ext = FileName.getExtension(fileName);
        RouteReader training = getReader(ext);
        if (training != null) {
            try {
                lastMessage = training.load(fileName);
            } catch (Exception ex) {
                lastMessage = ex.getLocalizedMessage();
                logger.error(lastMessage, ex);
            }
        } else {
            lastMessage = "Unknown reader " + ext;
        }

        // training loaded correctly, unregister previous one, and register
        // current.
        if (lastMessage == null) {
            // training is ready to be run
            currentTraining = training;
            if (currentTraining != null) {
                currentTraining.activate();
            }
        }
        return lastMessage;
    }

    /*
     * Telemetry handler interface
     */
    @Override
    public String getPrettyName() {
        return getCurrentTraining().getName();
    }

    @Override
    public void setPrettyName(String name) {
        assert false : "Cannot change the name";
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return getCurrentTraining().provides(data);
    }

    @Override
    public boolean checks(SourceDataEnum data) {
        return getCurrentTraining().checks(data);
    }

    @Override
    public double getValue(SourceDataEnum data) {
        return getCurrentTraining().getValue(data);
    }

    @Override
    public long getModificationTime(SourceDataEnum data) {
        return getCurrentTraining().getModificationTime(data);
    }

    @Override
    public long getLastMessageTime() {
        // trainings always activated.
        return -1;
    }

    @Override
    public SourceDataHandlerIntf initialize() {
		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);

        // notify about new telemetryProvider
        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        return this;
    }

    @Override
    public void release() {
        MessageBus.INSTANCE.send(Messages.HANDLER_REMOVED, this);

		MessageBus.INSTANCE.unregister(Messages.CLOSE, this);
        MessageBus.INSTANCE.unregister(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.unregister(Messages.CONFIG_CHANGED, this);
    }

    @Override
    public void callback(Messages m, Object o) {
        switch (m) {
            case TELEMETRY:
                synchronized(this) {
                    getTraining().storeTelemetryData((Telemetry) o);
                }
                break;
            case CONFIG_CHANGED:
                getTraining().configChanged((UserPreferences) o);
                break;
            case CLOSE:
                // close might be sent from menu, or from this class..
                if (currentTraining != null) {
                    currentTraining.close();
                    currentTraining = null;
                }
                break;
        }
    }
}