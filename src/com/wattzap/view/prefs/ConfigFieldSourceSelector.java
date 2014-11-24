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
package com.wattzap.view.prefs;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.EnumerationIntf;
import com.wattzap.model.SelectableDataSource;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.SourceDataHandlerIntf;
import com.wattzap.model.UserPreferences;

/**
 *
 * @author Jarek
 */
public class ConfigFieldSourceSelector extends ConfigFieldEnum implements MessageCallback {
    private final SelectableDataSource sources;

    private ConfigFieldSourceSelector(ConfigPanel panel, UserPreferences property, String name, SelectableDataSource dataEnum) {
        super(panel, property, name, dataEnum.getValues());
        this.sources = dataEnum;

        // check if list of handlers changes. Keep in mind, that configFields
        // are not removed from the panel (as well.. panels are not removed)
        // and this field stays registered forever
        MessageBus.INSTANCE.register(Messages.HANDLER, this);
        MessageBus.INSTANCE.register(Messages.HANDLER_REMOVED, this);
    }

    public ConfigFieldSourceSelector(ConfigPanel panel, UserPreferences property, String name, SourceDataEnum data) {
        this(panel, property, name, new SelectableDataSource(data));
    }

    @Override
    public void remove() {
        assert false : "Field cannot be removed";
    }

    @Override
    public EnumerationIntf getProperty() {
        String key = property.getString();
        for (EnumerationIntf en : sources.getValues()) {
            if (key.equals(en.getKey())) {
                return en;
            }
        }
        return null;
    }

    @Override
    public void setProperty(EnumerationIntf val) {
        property.setString(val.getKey());
    }

    @Override
    public void callback(Messages m, Object o) {
        boolean changed = false;
        switch (m) {
            case HANDLER:
                changed = sources.addHandler((SourceDataHandlerIntf) o);
                break;
            case HANDLER_REMOVED:
                changed = sources.removeHandler((SourceDataHandlerIntf) o);
                break;
        }
        if (changed) {
            setEnums(sources.getValues());
        }
    }
}
