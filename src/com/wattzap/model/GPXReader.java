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
package com.wattzap.model;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.jfree.data.xy.XYSeries;

import com.gpxcreator.gpxpanel.GPXFile;
import com.gpxcreator.gpxpanel.Track;
import com.gpxcreator.gpxpanel.Waypoint;
import com.gpxcreator.gpxpanel.WaypointGroup;
import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;
import com.wattzap.utils.FileName;
import com.wattzap.utils.Rolling;

/*
 * Wrapper class for GPX Track. Performs some analysis such as calculating
 * instantaneous speed, average gradient etc.
 *
 * @author David George (c) Copyright 2013
 * @date 11 June 2013
 */
@RouteAnnotation
public class GPXReader extends RouteReader {
    private double totalWeight = 85.0;
    private Power power = null;

    private GPXFile gpxFile;
	private XYSeries series;

    private File currentFile;

    private static final int gradientDistance = 100; // distance to calculate
														// gradients over.
	private double maxSlope = 0;
	private double minSlope = 0;

	private Point[] points = null;
	private int currentPoint = 0;
    private double lastDistance = 0.0;
    private String gpxName;

    @Override
	public String getExtension() {
		return "gpx";
	}

    @Override
    public TrainingModeEnum getMode() {
        return TrainingModeEnum.DISTANCE;
    }

    @Override
	public double getMaxSlope() {
		return maxSlope;
	}

	@Override
	public double getMinSlope() {
		return minSlope;
	}

    @Override
	public String getPath() {
		return currentFile.getParent();
	}

	@Override
	public String getVideoFile() {
		return FileName.stripExtension(currentFile.getName()) + ".avi";
	}

	@Override
	public String getName() {
		return gpxName;
	}

	@Override
	public GPXFile getGpxFile() {
		return gpxFile;
	}

    @Override
	public XYSeries getSeries() {
		return series;
	}

	// Get the point that corresponds to the distance (in km)
    // TODO getPoint() uses bisection, return values shall be mediana for distance
	@Override
	public Point getPoint(double distance) {
        if (lastDistance > distance) {
            currentPoint = 0;
        }
        lastDistance = distance;

        while ((currentPoint < points.length) && (points[currentPoint].getDistanceFromStart() < (distance * 1000))) {
			currentPoint++;
		}
        if (currentPoint >= points.length) {
            return null;
        } else if (currentPoint > 0) {
			return points[currentPoint - 1];
		} else {
			return points[0];
		}
	}

	/**
	 * Load GPX data from file
	 *
	 * @param filename
	 *            name of file to load
	 *
	 */
    @Override
	public String load(String filename) {
        currentFile = new File(filename);
        if (!currentFile.exists()) {
            return "File doesn't exist";
        }

        gpxFile = new GPXFile(currentFile);
        if (gpxFile == null) {
            return "Cannot read file";
        }

        gpxName = gpxFile.getName();

		List<Track> routes = gpxFile.getTracks();
		if (routes.size() == 0) {
			return "No tracks in file";
		}
        // TODO what if file contains multiple routes?
		Track route = routes.get(0);
		if (route == null) {
			return "no route in GPX file";
		}
        if (!route.getName().isEmpty()) {
            gpxName = route.getName();
        }

        List<WaypointGroup> segs = route.getTracksegs();
		this.series = new XYSeries("");

		double distance = 0.0;
		long startTime = System.currentTimeMillis();

		/*
		 * A GPX file can contain more than 1 segment. There may, or may not, be
		 * a distance gap between segments. For example, due to a tunnel. We
		 * treat each segment independently even if they correspond to a
		 * contiguous video.
		 */
		long lastSegTime = 0;
		for (WaypointGroup group : segs) {

			Rolling altitude = new Rolling(10);
			List<Waypoint> waypoints = group.getWaypoints();

			// group.correctElevation(true);
			Point[] segment = new Point[group.getNumPts()];

			Waypoint last = null;
			int index = 0;
			long currentTime = 0;
			for (Waypoint wp : waypoints) {
				Date d = wp.getTime();
				if (d != null) {
					currentTime = d.getTime();
				}

				if (index == 0) {
					last = wp;
					if (lastSegTime > 0 && currentTime > 0) {
						startTime += currentTime - lastSegTime;
					} else {
						startTime = currentTime;
					}
				}

				Point p = new Point();
				p.setElevation(wp.getEle());
				p.setLatitude(wp.getLat());
				p.setLongitude(wp.getLon());
				// TODO need to deduct gap time

				double leg = distance(wp.getLat(), last.getLat(), wp.getLon(),
						last.getLon(), last.getEle(), wp.getEle());
				distance += leg;
				p.setDistanceFromStart(distance);

				// smooth altitudes a bit
				altitude.add(wp.getEle());
				series.add(distance / 1000, altitude.getAverage());

				// speed = distance / time
				if (currentTime > 0) {
					p.setTime(currentTime - startTime);

					long t = currentTime - last.getTime().getTime();
					p.setSpeed((leg * 3600 / t));
				}
				segment[index++] = p;
				last = wp;
			}// for

			if (currentTime > 0) {
				lastSegTime = last.getTime().getTime();
			}
			// set initial speed
			segment[0].setSpeed(segment[1].getSpeed());

			/*
			 * Calculate the gradient, we do this using blocks of 100 meters
			 * using a moving average of 10 values.
			 */
			int i = 0;
			int j = 0;
			Rolling gradient = new Rolling(10);
			for (Point p : segment) {
				if (p.getDistanceFromStart() > segment[i]
						.getDistanceFromStart() + gradientDistance) {
					double slope = 100
							* (p.getElevation() - segment[i].getElevation())
							/ (p.getDistanceFromStart() - segment[i]
									.getDistanceFromStart());
					gradient.add(slope);
					if (slope > maxSlope) {
						maxSlope = slope;
					}
					if (slope < minSlope) {
						minSlope = slope;
					}
					segment[i++].setGradient(gradient.getAverage());
				}
				j++;
			}

			while (i < j - 1) {
				double slope = 100
						* (segment[j - 1].getElevation() - segment[i]
								.getElevation())
						/ (segment[j - 1].getDistanceFromStart() - segment[i]
								.getDistanceFromStart());
				gradient.add(slope);
				segment[i++].setGradient(gradient.getAverage());
			}
			segment[i++].setGradient(gradient.getAverage());
			// gradient done

			// resistance levels - use blocks of 500 meters
			// levels done

			// combine segment
			points = ArrayUtils.addAll(points, segment);
		}
        return null;
	}

    @Override
	public void close() {
        gpxFile = null;
        points = null;
        series = null;
		currentPoint = 0;
        lastDistance = 0;
	}

	/**
	 * Calculate distance between two points in latitude and longitude taking
	 * into account height difference. If you are not interested in height
	 * difference pass 0.0. Uses Haversine method as its base.
	 *
	 * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
	 * el2 End altitude in meters
     *
     * TODO move it to any Util class..
	 */
	public static double distance(double lat1, double lat2, double lon1,
			double lon2, double el1, double el2) {

		final int R = 6371; // Radius of the earth

		Double latDistance = deg2rad(lat2 - lat1);
		Double lonDistance = deg2rad(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		double height = el1 - el2;

		distance = Math.pow(distance, 2) + Math.pow(height, 2);

		return Math.sqrt(distance);
	}

	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case PAUSE: // pause when end of route
            case SPEED: // compute speed from power

            case ROUTE_SPEED:
            case ROUTE_TIME:
            case ALTITUDE:
            case SLOPE:
            case LATITUDE:
            case LONGITUDE:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        Point p = getPoint(t.getDistance());
        if (p != null) {
            double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                p.getGradient() / 100.0, t.getPower());
            setValue(SourceDataEnum.SPEED, realSpeed);

            setValue(SourceDataEnum.ROUTE_SPEED, p.getSpeed());
            setValue(SourceDataEnum.ROUTE_TIME, p.getTime());
            setValue(SourceDataEnum.ALTITUDE, p.getElevation());
            setValue(SourceDataEnum.SLOPE, p.getGradient());
            setValue(SourceDataEnum.LATITUDE, p.getLatitude());
            setValue(SourceDataEnum.LONGITUDE, p.getLongitude());
        }

        // set pause at end of route or when no running, otherwise unpause
        if (p == null) {
            setValue(SourceDataEnum.PAUSE, 100.0); // end of training
            setValue(SourceDataEnum.SPEED, 0.0);
        } else if (getValue(SourceDataEnum.SPEED) < 0.01) {
            if (t.getTime() < 1000) {
                setValue(SourceDataEnum.PAUSE, 1.0); // start training
            } else {
                setValue(SourceDataEnum.PAUSE, 2.0); // keep moving
            }
        } else {
            setValue(SourceDataEnum.PAUSE, 0.0); // running!
        }
    }

    @Override
    public void configChanged(UserPreferences pref) {
        if ((pref == UserPreferences.INSTANCE) || (pref == UserPreferences.TURBO_TRAINER)) {
            power = pref.getTurboTrainerProfile();
        }
        // it can be updated every configChanged without checking the property..
        totalWeight = pref.getTotalWeight();
    }
}
