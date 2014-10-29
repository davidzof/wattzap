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
import com.gpxcreator.gpxpanel.GPXFile;
import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TrainingItem;
import com.wattzap.utils.FileName;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
    private File currentFile = null;

    // data to show in profile chart
	private List<TrainingItem> training = null;
    private XYSeries series = null;
    private boolean providesCad;
    private boolean providesPower;
    private boolean providesHr;
    private double runningTime;
    private Map<SourceDataEnum, Integer> checks;
    private int lastItem;

    @Override
    public String getExtension() {
        return "trn";
    }

    @Override
    public String getPath() {
		return currentFile.getParent();
    }

    @Override
    public String getVideoFile() {
        // to be used with sufferfest?
		return FileName.stripExtension(currentFile.getName()) + ".avi";
    }

    @Override
    public String getName() {
        return FileName.stripExtension(currentFile.getName());
    }

    @Override
    public GPXFile getGpxFile() {
        return null;
    }

    @Override
    public XYSeries getSeries() {
        return series;
    }

    @Override
    public String getXKey() {
        return "time";
    }
    @Override
    public String getYKey() {
        return "power";
    }

    @Override
    public String load(String filename) {
        currentFile = new File(filename);
        if (!currentFile.exists()) {
            return "File doesn't exist";
        }

        CSVReader reader = null;
        training = new ArrayList<>();
        runningTime = 0.0;
        lastItem = -1;

        try {
            // The format is: Time, Comment, Heart Rate, Power, Cadence
            // TODO extend format to be used with header (to ommit some fields,
            // put them in different order, etc)
            reader = new CSVReader(new FileReader(filename));

            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                String f1 = nextLine[0];

                if (f1.trim().isEmpty() || f1.trim().startsWith("#")) {
                    continue;
                }
                TrainingItem item = new TrainingItem();
                item.setTime(runningTime);

                if (f1.indexOf(':') != -1) {
                    // minutes seconds
                    int minutes = Integer.parseInt(f1.substring(0,
                            f1.indexOf(':')));
                    int seconds = Integer.parseInt(f1.substring(f1
                            .indexOf(':') + 1));
                    runningTime += (minutes * 60.0) + seconds;
                } else {
                    runningTime += Double.parseDouble(f1) * 60;
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

        // prepare series to be displayed in profile panel, nice bars are nice!
        // only power is shown?
        series = new XYSeries("");
        for (int i = 0; i < training.size(); i++) {
            TrainingItem item = training.get(i);
            series.add(item.getTime(), item.getPower());
            if (i == training.size() - 1) {
                series.add(runningTime, item.getPower());
                // add zero to see "general" effort, not only spikes
                series.add(runningTime, 0);
            } else {
                series.add(training.get(i + 1).getTime(), item.getPower());
            }
        }

        checks = new HashMap<>();
        return null;
    }

    @Override
    public void close() {
        runningTime = 0;
        providesPower = false;
        providesCad = false;
        providesHr = false;
        currentFile = null;
        training = null;
        series = null;
        checks = null;
    }

    @Override
    public double getMaxSlope() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public double getMinSlope() {
        throw new UnsupportedOperationException("Not supported");
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

    private TrainingItem getItem(double time) {
        // collection is rather small, iterating is the fastes way.
        if (time < runningTime) {
            for (int i = training.size(); (i--) > 0; ) {
                TrainingItem item = training.get(i);
                if (item.getTime() <= time) {
                    if (i != lastItem) {
                        System.err.println("Start stage #" + i + ":: " + item);
                        String msg = item.getDescription();
                        if (msg != null) {
                            MessageBus.INSTANCE.send(Messages.ROUTE_MSG, msg);
                        }
                        lastItem = i;
                    }
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // handle telemetry for the time
        TrainingItem item = getItem(t.getDistance());
        if (item == null) {
            setValue(SourceDataEnum.PAUSE, 100.0);
            return;
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
        // the data related to functionalHR and FTP shall be rebuilt on property change?
        // Now just ignore it.
    }
}
