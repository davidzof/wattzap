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
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TrainingItem;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public String getExtension() {
        return "trn";
    }

    @Override
    public String load(File file) {
        AxisPointsList<TrainingItem> training = new AxisPointsList<>();
        this.training = null;
        routeLen = 0.0;

        CSVReader reader = null;
        // The default format is: Time, Comment, Heart Rate, Power, Cadence
        List<String> columns = Arrays.asList(new String[] {
            "interval", "comment" ,"hr", "power", "cadence"});
        int line = 0;
        int col = -1;
        try {
            reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            TrainingItem item = null;

            while ((nextLine = reader.readNext()) != null) {
                boolean definition = false;
                item = null;
                line++;

                for (col = 0; col < nextLine.length; col++) {
                    // nextLine[] is an array of values from the line
                    String f = nextLine[col].trim();
                    if ((col == 0) && (f.startsWith("#"))) {
                        break;
                    }
                    if ((col == 0) && (f.startsWith("!"))) {
                        columns = new ArrayList<>();
                        definition = true;
                        f = f.substring(1).trim();
                    }

                    if (definition) {
                        columns.add(f.toLowerCase());
                    } else if (!f.isEmpty()) {
                        if (col >= columns.size()) {
                            return "Column #" + col + " not defined, line " + line;
                        }
                        boolean ok = false;
                        double t;
                        switch (columns.get(col)) {
                            case "interval":
                            case "i":
                            case "int":
                                t = parseTime(f);
                                item = new TrainingItem(routeLen);
                                if (t > 0.0) {
                                    routeLen += t;
                                    ok = true;
                                }
                                break;
                            case "time":
                            case "t":
                                t = parseTime(f);
                                item = new TrainingItem(t);
                                if (t > routeLen) {
                                    routeLen = t;
                                    ok = true;
                                }
                                break;
                            case "hr":
                                if (item == null) {
                                    break;
                                }
                                providesHr = true;
                                ok = item.setHr(f);
                                break;
                            case "rpe":
                                if (item == null) {
                                    break;
                                }
                                providesPower = true;
                                ok = item.setRpe(f);
                                break;
                            case "power":
                                if (item == null) {
                                    break;
                                }
                                providesPower = true;
                                ok = item.setPower(f);
                                break;
                            case "cadence":
                            case "cad":
                            case "rpm":
                                if (item == null) {
                                    break;
                                }
                                providesCad = true;
                                ok = item.setCadence(f);
                                break;
                            case "comment":
                            case "msg":
                            case "info":
                                if (item == null) {
                                    break;
                                }
                                // add all fields till end, no trim() is applied
                                if (col == columns.size() - 1) {
                                    for (col++; col < nextLine.length; col++) {
                                        f += "," + nextLine[col];
                                    }
                                }
                                item.setDescription(f);
                                ok = true;
                                break;
                            default:
                                return "Unknown column \"" + columns.get(col) + "\"";
                        }
                        if (!ok) {
                            if (item == null) {
                                return "Line " + line + " doesn't have timestamp";
                            }
                            return "Incorrect data" +
                                    "; line " + line + ", column " + columns.get(col);
                        }
                    }
                }
                if (item != null) {
                    training.add(item);
                }
            }
            // if "ivterval" mode, add last point without data, just to show
            // segment time.
            if ((item != null) && (item.getDistance() < routeLen)) {
                training.add(new TrainingItem(routeLen));
            }
        } catch (Exception ex) {
            return ex.getMessage() + "; line " + line + ", column " + columns.get(col);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        String ret;
        if ((ret = training.checkData()) != null) {
            return ret;
        }

        checks = new HashMap<>();
        this.training = training;
        return null;
    }

    private double parseTime(String f) {
        String[] fields = f.split(":");
        try {
            if (fields.length == 1) {
                return Double.parseDouble(f) * 60.0;
            } else {
                double time = 0.0;
                for (int i = 0; i < fields.length; i++) {
                    time = 60.0 * time + Integer.parseInt(fields[i]);
                }
                return time;
            }
        } catch (NumberFormatException nfe) {
            return -1.0;
        }
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
        if (t.getDistance() > routeLen) {
            setPause(PauseMsgEnum.END_OF_ROUTE);
            return;
        }

        TrainingItem item = training.get(t.getDistance());
        // if point was passed.. show message
        if (training.isChanged() && (item.getDescription() != null)) {
            MessageBus.INSTANCE.send(Messages.ROUTE_MSG, item.getDescription());
        }

        // distance equals the time [s], route time in [ms]
        setValue(SourceDataEnum.ROUTE_TIME, t.getDistance() * 1000.0);
        // no pause
        setPause(PauseMsgEnum.RUNNING);
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
            TrainingItem.setFtp(pref.getMaxPower());
            if (providesPower) {
                reload = true;
            }
        }
        if (reload) {
            reloadTraining();
        }
    }
}
