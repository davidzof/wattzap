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
import com.wattzap.model.Constants;
import com.wattzap.model.SensorBuilder;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.SourceDataHandlerIntf;
import com.wattzap.model.SubsystemIntf;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Handles pairing, sensors, data selectors and their configs.
 *
 * @author David George
 * @date 25th August 2013
 * @author Jarek
 */
public class SensorsPanel extends ConfigPanel {
    private UserPreferences userPrefs = UserPreferences.INSTANCE;
    private Thread thread = null;
    private JPanel sensorPanel;

    public SensorsPanel() {
		super();
        // register MsgBundle
        MessageBus.INSTANCE.register(Messages.HANDLER, this);

        add(new ConfigFieldCheck(this, UserPreferences.ANT_ENABLED, "ant_enabled"));
        add(new ConfigFieldCheck(this, UserPreferences.ANT_USBM, "ant_usbm"));

        add(new ConfigFieldCheck(this, UserPreferences.PAIRING, "pairing") {
            @Override
            public void setProperty(boolean val) {
                userPrefs.setPairing(val);
                checking(val);
            }
        });

        // build panels for all existing sensors
        sensorPanel = new JPanel();
        sensorPanel.setBorder(new TitledBorder(MsgBundle.getString("defined_sensors")));
        MigLayout layout = new MigLayout();
		sensorPanel.setLayout(layout);
        add(sensorPanel, "span");

        SensorBuilder.buildFields(this);

        // selectors for speed/cadence/hr/power and additional params
        add(new ConfigFieldSourceSelector(this, UserPreferences.SPEED_SOURCE,
                "speed_source", SourceDataEnum.WHEEL_SPEED));
        // TODO build "custom" panels only when option was selected
        add(new ConfigFieldDouble(this, UserPreferences.ROBOT_SPEED, "robot_speed",
                "%.1f", "km/h", "mph", Constants.KMTOMILES));

        add(new ConfigFieldSourceSelector(this, UserPreferences.CADENCE_SOURCE,
                "cadence_source", SourceDataEnum.CADENCE));

        add(new ConfigFieldSourceSelector(this, UserPreferences.HR_SOURCE,
                "hr_source", SourceDataEnum.HEART_RATE));

        add(new ConfigFieldSourceSelector(this, UserPreferences.POWER_SOURCE,
                "power_source", SourceDataEnum.POWER));
        add(new ConfigFieldInt(this, UserPreferences.ROBOT_POWER, "robot_power", "W"));
	}

    public JPanel getSensorPanel() {
        return sensorPanel;
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
        // start all subsystems if not started..
        // TODO it must be smart somehow to avoid parinig when started
        // and vice versa..
        if (!userPrefs.isStarted()) {
            List<SubsystemIntf> subsystems = TelemetryProvider.INSTANCE.getSubsystems();
            for (SubsystemIntf subsystem : subsystems) {
                subsystem.open();
            }
        }

        List<ConfigFieldSensor> sensorFields = getSensorFields();
        while (!Thread.interrupted()) {
            for (ConfigFieldSensor sensorField : sensorFields) {
                sensorField.updateSensor();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        // stop all subsystems back
        if (!userPrefs.isStarted()) {
            List<SubsystemIntf> subsystems = TelemetryProvider.INSTANCE.getSubsystems();
            for (SubsystemIntf subsystem : subsystems) {
                subsystem.close();
            }
        }
    }

    // configuration changed callback
    @Override
    public void callback(Messages m, Object o) {
        if (m == Messages.HANDLER) {
            List<ConfigFieldSensor> sensorFields = getSensorFields();
            for (ConfigFieldSensor sensorField : sensorFields) {
                if (sensorField.getName().equals(((SourceDataHandlerIntf) o).getPrettyName())) {
                    sensorField.updateSensor();
                }
            }
        }
        super.callback(m, o);
    }

}
