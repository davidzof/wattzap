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
import com.wattzap.model.UserPreferences;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Jarek
 */
public class ConfigPanel extends JPanel implements ActionListener, DocumentListener, MessageCallback {
    private final Map<String, ConfigFieldIntf> fields = new HashMap<>();
    private String changedProperty;

    public ConfigPanel() {
        // register MsgBundle
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);

        // create layout
        MigLayout layout = new MigLayout();
		setLayout(layout);

        changedProperty = null;
    }

    public void add(ConfigFieldIntf field) {
        assert !fields.containsKey(field.getName()) :
            "Field " + field.getName() + " already in the panel";

        fields.put(field.getName(), field);
        // initialize values and labels, only in this field
        field.propertyChanged(UserPreferences.INSTANCE, null);
    }

    public List<ConfigFieldSensor> getSensorFields() {
        List<ConfigFieldSensor> list = new ArrayList<>();
        for (ConfigFieldIntf field : fields.values()) {
            if (field instanceof ConfigFieldSensor) {
                list.add((ConfigFieldSensor) field);
            }
        }
        return list;
    }

    private void fieldChanged(String name) {
        // if listener was called with no property or unknown one..
        if ((name == null) || (!fields.containsKey(name))) {
            return;
        }
        ConfigFieldIntf field = fields.get(name);
        if (field != null) {
            changedProperty =  name;
            field.fieldChanged();
            changedProperty = null;
        }
    }

    // callbacks for textFields
    @Override
    public void insertUpdate(DocumentEvent e) {
        fieldChanged((String) e.getDocument().getProperty("name"));
    }
    @Override
    public void removeUpdate(DocumentEvent e) {
        fieldChanged((String) e.getDocument().getProperty("name"));
    }
    @Override
    public void changedUpdate(DocumentEvent e) {
        fieldChanged((String) e.getDocument().getProperty("name"));
    }

    // callbacks for "normal" fields, actionCommand is a name of property
    @Override
    public void actionPerformed(ActionEvent e) {
        fieldChanged(e.getActionCommand());
    }

    // configuration changed callback
    @Override
    public void callback(Messages m, Object o) {
        if (m == Messages.CONFIG_CHANGED) {
            for (ConfigFieldIntf field : fields.values()) {
                field.propertyChanged((UserPreferences) o, changedProperty);
            }
        }
    }
}
