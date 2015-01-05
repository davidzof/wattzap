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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.wattzap.model.GPXReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.WorkoutData;
import com.wattzap.model.power.Power;

/**
 * Import TCX Format files
 *
 * @author David George
 * @date 2nd May 2014
 */
public class TcxImporter extends DefaultHandler {
	State currentState = State.UNDEFINED;
	StringBuilder buffer;
	protected static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private final SimpleDateFormat timestampFormatter;
	private ArrayList<Telemetry> data = null;
	private Telemetry point;
	private double distance = 0;

	private Rolling rSpeed = new Rolling(20);
	private Rolling pAve = new Rolling(20);
	private ExponentialMovingAverage gradeAve = new ExponentialMovingAverage(0.8);

	private final UserPreferences userPrefs = UserPreferences.INSTANCE;
	private static Logger logger = LogManager.getLogger("TCX Importer");

    public TcxImporter() {
		super();
		currentState = State.UNDEFINED;
		timestampFormatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
		data = new ArrayList<Telemetry>();
	}

	public ArrayList<Telemetry> getData() {
        return data;
    }
    /**
     * @return distance of whole route [km]
     */
    public double getDistance() {
        return distance;
    }

    public void startElement(String uri, String name, String qName,
			Attributes atts) {
		if (currentState == State.TRACKPOINT) {
			// Only if we are in a TRACKPOINT state can we enter any of these
			// states
			if ("Cadence".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("Time".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("DistanceMeters".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("AltitudeMeters".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("HeartRateBpm".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("Extensions".equalsIgnoreCase(name)) {
				currentState = State.EXTENSIONS;
			} else if ("Position".equalsIgnoreCase(name)) {
				currentState = State.POSITION;
			}
		} else if (currentState == State.EXTENSIONS) {
			if ("Watts".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("Speed".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			}
		} else if (currentState == State.POSITION) {
			if ("LatitudeDegrees".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			} else if ("LongitudeDegrees".equalsIgnoreCase(name)) {
				buffer = new StringBuilder();
			}
		} else if ("Trackpoint".equalsIgnoreCase(name)) {
			point = new Telemetry();

			currentState = State.TRACKPOINT;
		} else if ("DistanceMeters".equalsIgnoreCase(name)) {
			buffer = new StringBuilder();
		}
	}

	public void endElement(String uri, String name, String qName) {

		try {
			if (currentState == State.TRACKPOINT) {
				// Only if we are in a TRACKPOINT state can we enter any of
				// these states
				if ("Cadence".equalsIgnoreCase(name)) {
					int cadence = Integer.parseInt(buffer.toString().trim());
					point.setCadence(cadence);
				} else if ("Time".equalsIgnoreCase(name)) {
					String tt = buffer.toString().trim();
					Date d = timestampFormatter.parse(tt);
					point.setTime(d.getTime());
				} else if ("HeartRateBpm".equalsIgnoreCase(name)) {
					int hr = Integer.parseInt(buffer.toString().trim());
					point.setHeartRate(hr);
				} else if ("DistanceMeters".equalsIgnoreCase(name)) {
					double distance = Double.parseDouble(buffer.toString()
							.trim()) / 1000.0;
					point.setDistance(distance);
				} else if ("AltitudeMeters".equalsIgnoreCase(name)) {
					double altitude = Double.parseDouble(buffer.toString()
							.trim());
					point.setElevation(altitude);
				} else if ("Trackpoint".equalsIgnoreCase(name)) {
					// finalize data
					int current = data.size();
					if (current > 0) {
						Telemetry last = data.get(current - 1);

						double d = GPXReader.distance(point.getLatitude(),
								last.getLatitude(), point.getLongitude(),
								last.getLongitude(), point.getElevation(),
								last.getElevation());
                        // telemetry [km], d [m]
						distance += (d / 1000.0);
						point.setDistance(distance);

						if (!point.isAvailable(SourceDataEnum.SPEED)) {
							// calculate speed, s = d / t
							double speed = rSpeed.add(d /
									((point.getTime() - last.getTime()) / 1000.0));
                            // speed [m/s], in telemetry [km/h]
							point.setSpeed(3.6 * speed);
						} else {
                            // cumulate speed all the time, even if given as value!
                            rSpeed.add(point.getSpeed() / 3.6);
                        }

                        // altitude must be averaged a bit, otherwise gradient
                        // will be very stepy, thus power will be stepy as well
                        if (d > 0.1) {
                            double gradient = gradeAve.average((point.getElevation() -
                                    last.getElevation()) / d);
                            // telemetry [%], gradient [0..1]
                            point.setGradient(gradient * 100.0);
                        } else if (current > 1) {
                            point.setGradient(last.getGradient());
                        } else {
                            point.setGradient(0.0);
                        }

						if (!point.isAvailable(SourceDataEnum.POWER)) {
							int p = (int) pAve.add(Power.getPower(
									userPrefs.getTotalWeight(),
                                    point.getGradient() / 100.0,
									point.getSpeed()));

							if (p > userPrefs.getMaxPower()
									&& (p > (last.getPower() * 2.0))) {
								// We are above FTP and power has doubled,
								// remove power spikes
								p = (int) (last.getPower() * 1.05);
							}
							if (p > (userPrefs.getMaxPower() * 4)) {
								// power is 4 x FTP, this is a spike
								p = last.getPower();
							}
							if (p > 0) {
								point.setPower(p);
							}
						}
					} else {
						if (point.isAvailable(SourceDataEnum.POWER)) {
							point.setResistance(WorkoutData.POWERMETER);
						} else {
							point.setResistance(WorkoutData.GPS);
						}
					}
					data.add(point);

					currentState = State.UNDEFINED;
				}
			} else if (currentState == State.EXTENSIONS) {
				if ("Watts".equalsIgnoreCase(name)) {
					int power = Integer.parseInt(buffer.toString().trim());
					point.setPower(power);
				} else if ("Speed".equalsIgnoreCase(name)) {
                    // what does it represent? It doesn't match time/distance..
					double speed = Double.parseDouble(buffer.toString().trim());
					point.setSpeed(3.6 * speed);
				} else if ("Extensions".equalsIgnoreCase(name)) {
					currentState = State.TRACKPOINT;
				}
			} else if (currentState == State.POSITION) {
				if ("LatitudeDegrees".equalsIgnoreCase(name)) {
					double latitude = Double.parseDouble(buffer.toString().trim());
					point.setLatitude(latitude);
				} else if ("LongitudeDegrees".equalsIgnoreCase(name)) {
					double longitude = Double.parseDouble(buffer.toString().trim());
					point.setLongitude(longitude);
				} else if ("Position".equalsIgnoreCase(name)) {
					currentState = State.TRACKPOINT;
				}
			}
			if ("DistanceMeters".equalsIgnoreCase(name)) {
				distance = Double.parseDouble(buffer.toString().trim()) / 1000.0;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void characters(char ch[], int start, int length) {
		if (buffer != null) {
			buffer.append(ch, start, length);
		}
		// System.out.println("buffer " + buffer);
	}

	public enum State {
		TIME, HR, CADENCE, WATTS, SPEED, TRACKPOINT, EXTENSIONS, POSITION, UNDEFINED
	}
}
