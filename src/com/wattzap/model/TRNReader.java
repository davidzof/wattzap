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

import au.com.bytecode.opencsv.CSVReader;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.AxisPointsList;
import com.wattzap.model.dto.RouteMsg;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TrainingItem;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Jarek
 */
@RouteAnnotation
public class TRNReader extends RouteReader {
    // data to show in profile chart
	private AxisPointsList<TrainingItem> training = null;
    private boolean providesCad;
    private boolean providesPower;
    private boolean providesHr;
    private Map<SourceDataEnum, Integer> checks;
    private int lastItem;

    @Override
    public String getExtension() {
        return "trn";
    }

    @Override
    public String load(File file) {
        AxisPointsList<TrainingItem> training = new AxisPointsList<>();
        this.training = null;
        routeLen = 0.0;
        lastItem = -1;

        CSVReader reader = null;
        try {
            // The format is: Time, Comment, Heart Rate, Power, Cadence
            // TODO extend format to be used with header (to ommit some fields,
            // put them in different order, etc)
            reader = new CSVReader(new FileReader(file));

            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                String f1 = nextLine[0];

                if (f1.trim().isEmpty() || f1.trim().startsWith("#")) {
                    continue;
                }
                TrainingItem item = new TrainingItem(routeLen);

                if (f1.indexOf(':') != -1) {
                    // minutes seconds
                    int minutes = Integer.parseInt(f1.substring(0,
                            f1.indexOf(':')));
                    int seconds = Integer.parseInt(f1.substring(f1
                            .indexOf(':') + 1));
                    routeLen += (minutes * 60.0) + seconds;
                } else {
                    routeLen += Double.parseDouble(f1) * 60;
                }

                item.setDescription(nextLine[1]);
                if (!nextLine[2].isEmpty()) {
                    providesHr = true;
                    item.setHr(nextLine[2].trim());
                }
                if (!nextLine[3].isEmpty()) {
                    providesPower = true;
                    item.setPower(nextLine[3]);
                }
                if (!nextLine[4].isEmpty()) {
                    providesCad = true;
                    item.setCadence(nextLine[4]);
                }

                training.add(item);
            }
        } catch (Exception ex) {
            return ex.getMessage();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        checks = new HashMap<>();
        this.training = training;
        return null;
    }

    @Override
    public XYSeries createProfile() {
        // training without power.. nothing to be shown
        if (!providesPower) {
            return null;
        }

        // prepare series to be displayed in profile panel, nice bars are nice!
        XYSeries series = new XYSeries("time_min,power");
        for (int i = 0; i < training.size(); i++) {
            TrainingItem item = training.get(i);
            series.add(item.getDistance() / 60.0, item.getPower());
            if (i == training.size() - 1) {
                series.add(routeLen / 60.0, item.getPower());
            } else {
                series.add(training.get(i + 1).getDistance() / 60.0,
                        item.getPower());
            }
        }
        return series;
    }

    @Override
    public void close() {
        providesPower = false;
        providesCad = false;
        providesHr = false;
        training = null;
        checks = null;
    }

    /**
     * Reader calculates only TARGET values (power, cadence and HR). These are
     * shown in training chart.
     */
    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case PAUSE: // end of training
            case ROUTE_TIME: // TIME mode: distance equals route time
                return true;
            case TARGET_POWER:
                return providesPower;
            case TARGET_CADENCE:
                return providesCad;
            case TARGET_HR:
                return providesHr;
        }
        return false;
    }

    @Override
    public boolean checks(SourceDataEnum data) {
        return checks.containsKey(data);
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // training file is rebuilding, probably changed on config
        if (training == null) {
            return;
        }

        // handle telemetry for the time
        TrainingItem item = training.get(t.getDistance());
        if (item == null) {
            setValue(SourceDataEnum.PAUSE, 100.0);
            return;
        }

        // if point was passed.. show message
        if (training.isChanged() && (item.getDescription() != null)) {
            MessageBus.INSTANCE.send(Messages.ROUTE_MSG,
                    new RouteMsg(item.getDescription()));
        }

        // distance equals the time [s], route time in [ms]
        setValue(SourceDataEnum.ROUTE_TIME, t.getDistance() * 1000.0);
        // no pause
        setValue(SourceDataEnum.PAUSE, 0.0);
        checks.clear();
        if (providesPower) {
            setValue(SourceDataEnum.TARGET_POWER, item.getPower());
            checks.put(SourceDataEnum.POWER, item.isPowerInRange(t.getPower()));
        }
        if (providesCad) {
            setValue(SourceDataEnum.TARGET_CADENCE, item.getCadence());
            checks.put(SourceDataEnum.CADENCE, item.isCadenceInRange(t.getCadence()));
        }
        if (providesHr) {
            setValue(SourceDataEnum.TARGET_HR, item.getHr());
            checks.put(SourceDataEnum.HEART_RATE, item.isHRInRange(t.getHeartRate()));
        }
    }

    @Override
    public long getModificationTime(SourceDataEnum data) {
        if (checks.containsKey(data)) {
            return checks.get(data);
        }
        assert false : "Does't check " + data + " value";
        return 0;
    }


    @Override
    public void configChanged(UserPreferences pref) {
        boolean reload = false;
        if ((pref == UserPreferences.HR_MAX) || (pref == UserPreferences.INSTANCE)) {
            TrainingItem.setMaxHr(pref.getMaxHR());
            if (providesHr) {
                reload = true;
            }
        }
        if ((pref == UserPreferences.MAX_POWER) || (pref == UserPreferences.INSTANCE)) {
            TrainingItem.setMaxPower(pref.getMaxPower());
            if (providesPower) {
                reload = true;
            }
        }
        if (reload) {
            reload();
        }
    }
}
