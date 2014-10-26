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
package com.wattzap.view;

import com.wattzap.MsgBundle;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;


import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TelemetryValidityEnum;
import java.util.ArrayList;
import java.util.List;

/*
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 */
public class Odo extends JPanel implements MessageCallback {
    private static final Color textColor = new Color(240, 244, 112);
	private static final Color skyBlue = new Color(0, 154, 237);
    private static final int style1 = Font.CENTER_BASELINE;
    private static final Font font1 = new Font("Arial", style1, 13);

    private static final int style = Font.BOLD | Font.ITALIC;
    private static final Font font = new Font("Arial", style, 30);

    private boolean paused = true;
    private boolean metric = true;
    private boolean rebuild = false;

    private static final Color[] colors = new Color[] {
        null, Color.GRAY, Color.WHITE, Color.RED, skyBlue, Color.GRAY
    };
    static {
        assert colors.length == TelemetryValidityEnum.values().length :
                "Wrong number of colors for validity";
    }

    private class ValueCol {
        private final SourceDataEnum sourceData;
        private JLabel label;
        private JLabel text;
        private TelemetryValidityEnum valid;

        private ValueCol(SourceDataEnum sourceData) {
            assert sourceData != null :
                    "SourceDataEnum not given";
            assert sourceData.getName() != null :
                    sourceData + " has no name, cannot be used";

            this.sourceData = sourceData;
            this.label = null;
            this.text = null;
            this.valid = TelemetryValidityEnum.NOT_PRESENT;
        }

        public void setValue(Telemetry t) {
            TelemetryValidityEnum valid = t.getValidity(sourceData);
            if ((valid != TelemetryValidityEnum.NOT_PRESENT) && paused) {
                valid = TelemetryValidityEnum.NOT_AVAILABLE;
            }
            if (this.valid != valid) {
                if (valid != TelemetryValidityEnum.NOT_PRESENT) {
                    if (label == null) {
                        label = new JLabel();
                        label.setFont(font1);
                        label.setForeground(textColor);
                        label.setText(MsgBundle.getString(sourceData.getName()));
                        text = new JLabel();
                        text.setFont(font);
                    }
                    if (this.valid == TelemetryValidityEnum.NOT_PRESENT) {
                        rebuild = true;
                    }
                    text.setForeground(colors[valid.ordinal()]);
                } else {
                    rebuild = true;
                }
                this.valid = valid;
            }
            if (text != null) {
                text.setText(sourceData.format(t.getDouble(sourceData), metric));
            }
        }

        public boolean isVisible() {
            return valid != TelemetryValidityEnum.NOT_PRESENT;
        }
        public JLabel getLabel() {
            return label;
        }
        public JLabel getText() {
            return text;
        }
    }

    private final List<ValueCol> columns = new ArrayList<>();

	public Odo() {
		super();

		setBackground(Color.BLACK);
		MigLayout layout = new MigLayout("fillx", "[center]", "[][shrink 0]");
		setLayout(layout);

        columns.add(new ValueCol(SourceDataEnum.SPEED));
        columns.add(new ValueCol(SourceDataEnum.WHEEL_SPEED));
        columns.add(new ValueCol(SourceDataEnum.DISTANCE));
        columns.add(new ValueCol(SourceDataEnum.POWER));
        columns.add(new ValueCol(SourceDataEnum.HEART_RATE));
        columns.add(new ValueCol(SourceDataEnum.CADENCE));
        columns.add(new ValueCol(SourceDataEnum.RESISTANCE));
        columns.add(new ValueCol(SourceDataEnum.SLOPE));
        columns.add(new ValueCol(SourceDataEnum.ALTITUDE));
        columns.add(new ValueCol(SourceDataEnum.TIME));

        // fill with "empty" values
        callback(Messages.CONFIG_CHANGED, UserPreferences.INSTANCE);
        callback(Messages.TELEMETRY, new Telemetry(-1));

        MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
	}

    @Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case TELEMETRY:
                Telemetry t = (Telemetry) o;
                paused = (TelemetryProvider.pauseMsg(t) != null);
                int num = 0;
                int last = 0;
                for (ValueCol column : columns) {
                    num++;
                    column.setValue(t);
                    if (column.isVisible()) {
                        last = num;
                    }

                }
                if (rebuild) {
                    rebuild = false;
                    if (getComponentCount() != 0) {
                        removeAll();
                    }
                    num = 0;
                    for (ValueCol column : columns) {
                        num++;
                        if (column.isVisible()) {
                            if (num < last) {
                                add(column.getLabel());
                            } else {
                                add(column.getLabel(), "Wrap");
                            }
                        }
                    }
                    num = 0;
                    for (ValueCol column : columns) {
                        num++;
                        if (column.isVisible()) {
                            add(column.getText());
                        }
                    }
                    revalidate();
                }
                break;
            case CONFIG_CHANGED:
                UserPreferences pref = (UserPreferences) o;
                if ((pref == UserPreferences.METRIC) ||
                        pref == UserPreferences.INSTANCE) {
                    metric = pref.isMetric();
                }
                break;
        }
	}
}
