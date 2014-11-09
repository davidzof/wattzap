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

import com.wattzap.model.power.HandlerEnum;
import com.wattzap.utils.ReflexiveClassLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all selectable profiles to be used in ConfigFieldSourceSelector
 * panel (in configPanel). It builds all profiles (strategies?) from this
 * package, other are rather sensors (and are created somewhere else).
 * This collect all existing handlers (for sourceData), all these handlers
 * are shown in the combo.
 *
 * @author Jarek
 */
public class SelectableDataSource {
    public final static EnumerationIntf none = new EnumerationIntf() {
        @Override
        public String getKey() {
            return "none";
        }

        @Override
        public int ordinal() {
            return 0;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public String toString() {
            return "Source[none]";
        }
    };


    public static void buildHandlers() {
		String packageName = SelectableDataSource.class.getPackage().getName();
        buildHandlers(packageName);
    }

    private static void buildHandlers(String packageName) {
        List<Class> classes;
        try {
            classes = ReflexiveClassLoader.getClassNamesFromPackage(
                    packageName, SelectableDataSourceAnnotation.class);
        } catch (IOException ioe) {
            assert false : "Cannot read package " + packageName;
            return;
        } catch (URISyntaxException ue) {
            assert false : "Wrong URI " + packageName;
            return;
        } catch (ClassNotFoundException nfe) {
            assert false : "Class not found " + packageName;
            return;
        } catch (InstantiationException | IllegalAccessException ie) {
            assert false : "Cannot create class from " + packageName;
            return;
        }

        for (Class clazz : classes) {
            SourceDataHandlerIntf handler = null;
            try {
                handler = (SourceDataHandlerIntf) clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ie) {
                assert false : "Cannot create " + clazz.getCanonicalName();
            }
            if (handler != null) {
                handler.initialize();
            }
        }
    }

    // each profile has own handlers, this is providing "current" source data
    private final List<HandlerEnum> handlers;
    private final SourceDataEnum source;

    public SelectableDataSource(SourceDataEnum source) {
        this.source = source;
        handlers = new ArrayList<>();
        for (SourceDataHandlerIntf handler : TelemetryProvider.INSTANCE.getHandlers()) {
            addHandler(handler);
        }
    }

    public EnumerationIntf[] getValues() {
        if (handlers.size() == 0) {
            return new EnumerationIntf[] {none};
        }
        return handlers.toArray(new EnumerationIntf[handlers.size()]);
    }

    // add handler on handler notification
    // If handler started providing source data later, it will send
    // another handler message (and will be added then).
    public boolean addHandler(SourceDataHandlerIntf handler) {
        // handler doesn't provide source information..
        if (!handler.provides(source)) {
            return false;
        }
        // check if already added
        for (HandlerEnum en : handlers) {
            if (en.getHandler() == handler) {
                // handler already added
                return false;
            }
        }
        // add new handler
        handlers.add(new HandlerEnum(this, handler));
        return true;
    }

    // remove handler on handlerRemoved notification
    public boolean removeHandler(SourceDataHandlerIntf handler) {
        boolean result = false;
        for (int i = handlers.size(); (i--) > 0; ) {
            if (handlers.get(i).getHandler() == handler) {
                handlers.remove(i);
                result = true;
            }
        }
        return result;
    }
}
