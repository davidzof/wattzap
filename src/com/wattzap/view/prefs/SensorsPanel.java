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
package com.wattzap.view.prefs;

import com.wattzap.MsgBundle;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.EnumerationIntf;
import com.wattzap.model.SensorTypeEnum;
import com.wattzap.model.SensorIntf;
import com.wattzap.model.SourceDataHandlerIntf;
import com.wattzap.model.SubsystemIntf;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import java.awt.Color;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * Handles pairing, sensors, data selectors and their configs.
 *
 * @author David George
 * @date 25th August 2013
 * @author Jarek
 */
public class SensorsPanel extends ConfigPanel {
    private static final UserPreferences userPrefs = UserPreferences.INSTANCE;

    private static final String addStr = "add";
    private static final String deleteStr = "delete";
    private static final String updateStr = "update";
    private static final String nameStr = "name";


    private final JTextField sensorName;
    private final ButtonGroup group;
    private final JRadioButton button;

    private final static int addButton = 0;
    private final static int updateButton = 1;
    private final static int deleteButton = 2;
    private final JButton[] buttons = new JButton[3];
    private int buttonState = -1;

    private Thread thread = null;
    private SensorIntf selected = null;
    private int lastId = 0;

    public SensorsPanel() {
		super();
        MessageBus.INSTANCE.register(Messages.HANDLER, this);
        MessageBus.INSTANCE.register(Messages.HANDLER_REMOVED, this);

        add(new ConfigFieldCheck(this, UserPreferences.ANT_ENABLED, "ant_enabled"));
        add(new ConfigFieldCheck(this, UserPreferences.ANT_USBM, "ant_usbm"));

        add(new ConfigFieldCheck(this, UserPreferences.PAIRING, "pairing") {
            @Override
            public void setProperty(boolean val) {
                userPrefs.setPairing(val);
                checking(val);
            }
        });

        // sensor builder, it consist of two lines:
        // - radio, name edit field, combo with type
        // - add/update/remove buttons
		group = new ButtonGroup();

        // Button to select editor row to add new group
        button = new JRadioButton("");
        button.setEnabled(true);
        button.setActionCommand("@*");
        button.addActionListener(this);
        group.add(button);
        add(button, "split 2");

        // name editor
		sensorName = new JTextField(10);
        sensorName.getDocument().putProperty("name", "!" + nameStr);
        sensorName.getDocument().addDocumentListener(this);
        add(sensorName);

        add(new ConfigFieldEnum(this, UserPreferences.SENSOR_TYPE, null,
                SensorTypeEnum.values()) {
            @Override
            public EnumerationIntf getProperty() {
                return userPrefs.getSensorType();
            }
            @Override
            public void setProperty(EnumerationIntf val) {
                userPrefs.setSensorType((SensorTypeEnum) val);
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(buttons[updateButton] = createButton(updateStr));
        buttonsPanel.add(buttons[addButton] = createButton(addStr));
        buttonsPanel.add(buttons[deleteButton] = createButton(deleteStr));
        add(buttonsPanel, "span, south");
        // all buttons are available by default, so "hide" them
        handleAction(null);

        // add panels for all sensors in configuration. All must be
        // correctly defined.
        List<String> sensors = UserPreferences.SENSORS.getSensors();
        for (String sensor : sensors) {
            SensorTypeEnum type = UserPreferences.SENSORS.getSensorType(sensor);
            add(new ConfigFieldSensor(this, group, sensor, type.getDefaultData()));
        }
        // all panels added, select editor
        button.setSelected(true);
	}
    private JButton createButton(String name) {
        JButton button = new JButton(MsgBundle.getString(name));
        button.setActionCommand("!" + name);
        button.addActionListener(this);
        return button;
    }

    private SensorIntf getSensor(String name) {
        List<SourceDataHandlerIntf> handlers = TelemetryProvider.INSTANCE.getHandlers();
        for (SourceDataHandlerIntf handler : handlers) {
            if ((handler instanceof SensorIntf) && (handler.getPrettyName().equals(name))) {
                return (SensorIntf) handler;
            }
        }
        return null;
    }

    public void checking(boolean enabled) {
        if (enabled) {
            if (thread == null) {
                thread = new Thread() {
                    public void run() {
                        sensorThread();
                    }
                };
                thread.start();
            }
        } else {
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
    }

    private void sensorThread() {
        List<SubsystemIntf> subsystems = TelemetryProvider.INSTANCE.getSubsystems();

        // start all subsystems. If training is started, they are for sure
        // started and it shows warning in the log.
        for (SubsystemIntf subsystem : subsystems) {
            subsystem.open();
        }

        List<ConfigFieldSensor> sensorFields = getSensorFields();
        for (;;) {
            for (ConfigFieldSensor sensorField : sensorFields) {
                sensorField.updateSensor();
            }
            if (Thread.interrupted()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        // stop all subsystems back, if not started. Otherwise they are
        // already closed, but who cares (only entry in the log again..)
        if (!userPrefs.isStarted()) {
            for (SubsystemIntf subsystem : subsystems) {
                subsystem.close();
            }
        }
    }

    // configuration changed callback
    @Override
    public void callback(Messages m, Object o) {
        boolean repaint = false;

        if (m == Messages.HANDLER) {
            SensorTypeEnum type = SensorTypeEnum.byClass(o.getClass());
            if (type != null) {
                SensorIntf sensor = (SensorIntf) o;
                boolean found = false;
                List<ConfigFieldSensor> sensorFields = getSensorFields();
                for (ConfigFieldSensor sensorField : sensorFields) {
                    if (sensorField.getName().equals("*" + sensor.getPrettyName())) {
                        sensorField.updateSensor();
                        found = true;
                    }
                }
                if (!found) {
                    add(new ConfigFieldSensor(this, group, sensor.getPrettyName(), type.getDefaultData()));
                    repaint = true;
                }
            }
        } else if (m == Messages.HANDLER_REMOVED) {
            List<ConfigFieldSensor> sensorFields = getSensorFields();
            for (ConfigFieldSensor sensorField : sensorFields) {
                if (sensorField.getName().equals("*" +
                        ((SourceDataHandlerIntf) o).getPrettyName())) {
                    remove(sensorField);
                    repaint = true;
                }
            }
        }
        if (repaint) {
            validate();
        }
        super.callback(m, o);
    }

    // handles !name (editor name changed), !type (editor type changed),
    // !{button} (button pressed) and @sensor (sensor line selected) actions
    @Override
    protected void fieldChanged(String name) {
        super.fieldChanged(name);
        switch (name.charAt(0)) {
            case '!':
                handleAction(name.substring(1));
                break;
            case '@':
                sensorSelected(name.substring(1));
                break;
        }
    }

    private final String getNameIfValid() {
        String text = sensorName.getText();
        System.err.println("Name changed, new " + text);

        boolean wrong = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i > 15) {
                // name too long
                wrong = true;
            } else if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))) {
                // leters are always ok
            } else if (((c >= '0') && (c <= '9')) || (c == '_')) {
                // digits and underscore not allowed as first char
                if (i == 0) {
                    wrong = true;
                }
            } else {
                // special chars not allowed
                wrong = true;
            }
        }
        if (text.isEmpty()) {
            wrong = true;
        }
        if ((!wrong) && ((selected == null) || (!text.equals(selected.getPrettyName())))) {
            // check if sensor exists
            SensorIntf sensor = getSensor(text);
            if (sensor != null) {
                wrong = true;
            }
        }
        if (wrong) {
            return null;
        } else {
            return text;
        }
    }

    private final void handleAction(String action) {
        String editorName = getNameIfValid();

        if (nameStr.equals(action)) {
            if (editorName != null) {
                sensorName.setBackground(Color.WHITE);
            } else {
                sensorName.setBackground(Color.RED);
            }
        }

        // To update sensor.. It must be removed and added once again
        if ((deleteStr.equals(action)) || (updateStr.equals(action))) {
            userPrefs.removeSensor(selected.getPrettyName());
            lastId = selected.getSensorId();
            // sensor is released, so it should disappear from the interface
            selected.release();
            selected = null;
            // select editor line
            button.setSelected(true);
        }
        if ((addStr.equals(action)) || (updateStr.equals(action))) {
            userPrefs.setSensor(editorName, userPrefs.getSensorType(), lastId);
            lastId = 0;
            // and in second new one is created, on callback it appears
            // automagically. Best would be, if new configField is added in
            // the place of previous one, but not an issue.
            selected = SensorTypeEnum.buildSensor(editorName);
        }

        // check available buttons
        int buttonState = 0;
        if (selected == null) {
            if (editorName != null) {
                buttonState |= (1 << addButton);
            }
        } else {
            String sName = sensorName.getText();
            if ((!sName.equals(selected.getPrettyName())) ||
                    (SensorTypeEnum.byClass(selected.getClass()) != userPrefs.getSensorType())) {
                if (editorName != null) {
                    buttonState |= (1 << updateButton);
                }
            }
            buttonState |= (1 << deleteButton);
        }
        // enable (or disable) buttons
        for (int b = 0; b < buttons.length; b++) {
            if ((this.buttonState < 0) ||
                    (this.buttonState & (1 << b)) != (buttonState & (1 << b))) {
                buttons[b].setEnabled((buttonState & (1 << b)) != 0);
            }
        }
        this.buttonState = buttonState;
    }

    private void sensorSelected(String name) {
        if (!name.equals("*")) {
            selected = getSensor(name);
            if (selected == null) {
                System.err.println("No requested sensor exist!");
                return;
            }
            // calls handleAction(name)
            sensorName.setText(selected.getPrettyName());
            userPrefs.setSensorType(SensorTypeEnum.byClass(selected.getClass()));
        } else {
            // update
            selected = null;
            handleAction(nameStr);
        }
    }
}
