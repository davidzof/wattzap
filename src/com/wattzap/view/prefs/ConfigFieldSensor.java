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
import com.wattzap.model.SensorIntf;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.SourceDataHandlerIntf;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import java.awt.Color;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Panel for sensor build on additional panel for sensors.
 * It handles callbacks for handlers.
 * @author Jarek
 */
public class ConfigFieldSensor implements ConfigFieldIntf, MessageCallback {
    private final ConfigPanel panel;
    private final ButtonGroup group;
    private final String name;
    private final SourceDataEnum data;
    private SensorIntf sensor;

    private final JLabel label;
    private final JRadioButton button;
    private final JTextField value;
    private final JLabel current;

    public ConfigFieldSensor(ConfigPanel panel, ButtonGroup group, String name) {
        this(panel, group, name, null);
    }

    public ConfigFieldSensor(ConfigPanel panel, ButtonGroup group, String name,
            SourceDataEnum data) {
        this.panel = panel;
        this.group = group;
        this.name = name;
        this.data = data;

        // initialize the sensor to change id
        // It must be deffered a bit to handle all notifications properly
        // (TelemetryProvider handles same messages as other, but queue is one
        sensor = null;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (SourceDataHandlerIntf handler : TelemetryProvider.INSTANCE.getHandlers()) {
                    if ((handler instanceof SensorIntf) &&
                            (name.equals(handler.getPrettyName()))) {
                        sensor = (SensorIntf) handler;
                        updateSensor();
                        break;
                    }
                }
            }
        });

        // build the interface
        if (group == null) {
            button = null;
            label = new JLabel();
            label.setText(name);
    		panel.add(label);
        } else {
            label = null;
            button = new JRadioButton(name);
            button.setEnabled(true);
            button.setActionCommand("@" + name);
            button.addActionListener(panel);
            group.add(button);
            button.setSelected(true);
            panel.add(button);
        }

        value = new JTextField(6);
        value.getDocument().putProperty("name", getName());
        value.getDocument().addDocumentListener(panel);

        if ((data != null) && (data.format(0.0, true) != null)) {
            current = new JLabel();
    		panel.add(value);
    		panel.add(current, "span");
        } else {
            current = null;
    		panel.add(value, "span");
        }

        MessageBus.INSTANCE.register(Messages.HANDLER_REMOVED, this);
    }

    @Override
    public void remove() {
        MessageBus.INSTANCE.unregister(Messages.HANDLER_REMOVED, this);

        if (button != null) {
            group.remove(button);
            panel.remove(button);
            button.removeActionListener(panel);
        }
        if (label != null) {
            panel.remove(label);
        }

        panel.remove(value);
        value.getDocument().removeDocumentListener(panel);

        if (current != null) {
            panel.remove(current);
        }
    }

    @Override
    public String getName() {
        // must be same as during registration in value field..
        return "*" + name;
    }

    @Override
    public void propertyChanged(UserPreferences prop, String changed) {
        // "ignore" changes made in the field directly
        if ((prop == UserPreferences.SENSORS) && (getName().equals(changed))) {
            return;
        }

        // block the interface if pairing is enabled (or everything is running..)
        if ((prop == UserPreferences.PAIRING) ||
                (prop == UserPreferences.RUNNING) ||
                (prop == UserPreferences.INSTANCE)) {
            //value.setEditable(!(prop.isPairingEnabled() || prop.isStarted()));
        }

        if (((prop == UserPreferences.SENSORS) && (name.equals(prop.getString())))
                || (prop == UserPreferences.INSTANCE)) {
            value.setText(String.format("%d", prop.getSensorId(name)));
        }
    }


    @Override
    public void fieldChanged() {
        int val = 0;
        boolean valid;
        try {
            val = Integer.parseInt(value.getText());
            valid = isValid(val);
        } catch (NumberFormatException nfe) {
            valid = false;
        }
        if (valid) {
            value.setBackground(Color.WHITE);
            UserPreferences.SENSORS.setSensorId(name, val);
        } else {
            value.setBackground(Color.RED);
        }
    }
    public boolean isValid(int val) {
        return (val >= 0) && (val < 65536);
    }

    public final void updateSensor() {
        if (current == null) {
            return;
        }

        if (sensor == null) {
            current.setText("missing");
            return;
        }

        // if sensor doesn't work..
        if (sensor.getLastMessageTime() == 0) {
            current.setText("");
            return;
        }
        // if requested data is not provided..
        if (!sensor.provides(data)) {
            current.setText("no data");
            return;
        }
        // if sensor paired, just set sensor id..
        if ((sensor.getSensorId() != 0) && (UserPreferences.SENSORS.getSensorId(name) == 0)) {
            UserPreferences.SENSORS.setSensorId(name, sensor.getSensorId());
        }
        // don't show value if sensor ID is different than
        if (sensor.getSensorId() != UserPreferences.SENSORS.getSensorId(name)) {
            current.setText("id conflict");
            return;
        }

        if (sensor.getModificationTime(data) + 10000 < System.currentTimeMillis()) {
            current.setText("not updated");
            return;
        }

        boolean metric = UserPreferences.METRIC.isMetric();
        current.setText(
            data.format(sensor.getValue(data), metric) + " " + data.getUnit(metric));
    }

    @Override
    public void callback(Messages m, Object o) {
        if ((m == Messages.HANDLER_REMOVED) && (sensor == o) && (sensor != null)) {
            // remove configField from sensorPanel, object is ready to be
            // garbaged
            remove();
        }
    }
}
