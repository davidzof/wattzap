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
import java.util.HashMap;
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
    private final Map<UserPreferences, ConfigFieldIntf> fields = new HashMap<>();
    private UserPreferences changedProperty;

    public ConfigPanel() {
        // register messages
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);

        // create layout
        MigLayout layout = new MigLayout();
		setLayout(layout);

        changedProperty = null;
    }

    public void add(ConfigFieldIntf field) {
        if ((field.getProp() == null) || (field.getProp() == UserPreferences.INSTANCE)) {
            throw new UnsupportedOperationException("Invalid property for field");
        }
        fields.put(field.getProp(), field);
        // initialize values and labels, only in this field
        field.propertyChanged(UserPreferences.INSTANCE, null);
    }

    private void fieldChanged(UserPreferences property) {
        // if listener was called with wrong property..
        if ((property == null) || (property == UserPreferences.INSTANCE)) {
            return;
        }
        ConfigFieldIntf field = fields.get(property);
        if (field != null) {
            changedProperty =  property;
            field.fieldChanged();
            changedProperty = null;
        }
    }

    // callbacks for textFields
    @Override
    public void insertUpdate(DocumentEvent e) {
        fieldChanged((UserPreferences) e.getDocument().getProperty("prop"));
    }
    @Override
    public void removeUpdate(DocumentEvent e) {
        fieldChanged((UserPreferences) e.getDocument().getProperty("prop"));
    }
    @Override
    public void changedUpdate(DocumentEvent e) {
        fieldChanged((UserPreferences) e.getDocument().getProperty("prop"));
    }

    // callbacks for "normal" fields, actionCommand is a name of property
    @Override
    public void actionPerformed(ActionEvent e) {
        UserPreferences p = UserPreferences.valueOf(null);
        fieldChanged(UserPreferences.valueOf(e.getActionCommand()));
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
