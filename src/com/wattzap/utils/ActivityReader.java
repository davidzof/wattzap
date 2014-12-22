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
package com.wattzap.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.WorkoutData;
import com.wattzap.view.training.TrainingAnalysis;
import java.io.File;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Imports Training Activities into the Wattzap database
 *
 * @author David George
 * @date 2nd May 2014
 */
public class ActivityReader  {
	String workoutDir;
	List<String> importedFiles = new ArrayList<String>();
	private Logger logger = LogManager.getLogger("GPSFileVisitor");


	public List<String> getImportedFileList() {
		return importedFiles;
	}

	public void readActivity(String fileName) {
        List<Telemetry> telemetry = readTelemetry(fileName);
        if (telemetry != null) {

            String workoutName = TcxWriter.getWorkoutName(telemetry
                    .get(0).getTime());
            WorkoutData workout = UserPreferences.INSTANCE
                    .getWorkout(workoutName);
            int dataSource = telemetry.get(0).getResistance();

            if (workout != null) {
                logger.info("File already in database "
                        + workout.getTcxFile());
            } else {
                workout = TrainingAnalysis.analyze(telemetry);
                workout.setFtp(UserPreferences.INSTANCE.getMaxPower());
                workout.setTcxFile(workoutName);
                workout.setSource(dataSource);

                TcxWriter writer = new TcxWriter();
                importedFiles.add(writer.save(telemetry, true));

                UserPreferences.INSTANCE.addWorkout(workout);
            }
        }
	}

    private static boolean importXml(DefaultHandler handler, String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        }

        XMLReader xmlReader;
        try {
            xmlReader = XMLReaderFactory.createXMLReader();
        } catch (SAXException ex) {
            return false;
        }
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);

        try {
            FileReader fileReader = new FileReader(file);
			xmlReader.parse(new InputSource(fileReader));
			fileReader.close();
            return true;
        } catch (IOException | SAXException ex) {
            return false;
        }
    }

    public static ArrayList<Telemetry> readTelemetry(String fileName) {
		if (fileName.endsWith(".tcx")) {
			TcxImporter handler = new TcxImporter();
            if (!importXml(handler, fileName)) {
                return null;
            }
            return handler.getData();
		} else if (fileName.endsWith(".fit")) {
			FitImporter handler = new FitImporter(fileName);
			return handler.data;
		} else if (fileName.endsWith(".gpx")) {
			GpxImporter handler = new GpxImporter();
            if (!importXml(handler, fileName)) {
                return null;
            }
            ArrayList<Telemetry> data = handler.data;

            FitlogImporter flHandler = new FitlogImporter();
            if (importXml(flHandler, fileName + "fitlog")) {
				ArrayList<Telemetry> fitData = flHandler.data;
				long first = data.get(0).getTime();
				int count = 0;
				for (Telemetry t : data) {
					if (count == fitData.size()) {
						break; // not enough data?
					}
					long time = t.getTime() - first;
					Telemetry fl = fitData.get(count);
					if (time < fl.getTime()) {
						t.setCadence(fl.getCadence());
						t.setHeartRate(fl.getHeartRate());
					} else {
						t.setCadence(fl.getCadence());
						t.setHeartRate(fl.getHeartRate());
						count++;
					}
				}
				data.get(0).setResistance(WorkoutData.FITLOG);
			}
    		return data;
		} else {
            // unhandled extension
            return null;
        }
	}
}
