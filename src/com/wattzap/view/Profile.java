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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.Constants;
import com.wattzap.model.RouteReader;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.OpponentData;
import com.wattzap.model.dto.Telemetry;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.RectangleEdge;

/*
 * Shows a profile of the route and moves an indicator to show rider progress on profile
 *
 * @author David George (c) Copyright 2013
 * @date 19 June 2013
 *
 * @author Jarek
 * Some small improvements. Profile shows any profile (this is defined by current
 * RouteReader). It might be distance/altitude, time/power..
 */
public class Profile extends JPanel implements MessageCallback, ChartMouseListener {
	private static final Logger logger = LogManager.getLogger("Profile");
    private static final Map<String, Double> valCorrections = new HashMap<>();
    static {
        // normal distance trainig, distance in in [km]
        valCorrections.put("distance_km", 1.0);
        valCorrections.put("distance_mi", Constants.KMTOMILES);
        // for time trainings, distance is time [s]
        valCorrections.put("time_min", 60.0);
        // altitude is in [m]
        valCorrections.put("altitude_m", 1.0);
        valCorrections.put("altitude_feet", Constants.MTOFEET);
    }

    private static final NumberFormat timeFormat = new NumberFormat() {
        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            int min = (int) number;
            number -= min;
            int sec = (int) (number * 60);
            StringBuffer buf = new StringBuffer();
            buf.append(min);
            buf.append(':');
            if (sec < 10) {
                buf.append('0');
            }
            buf.append(sec);
            return buf;
        }
        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return null;
        }
        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            return null;
        }
    };

    private ValueMarker marker = null;
	private XYPlot plot = null;
	private ChartPanel chartPanel = null;
    private String xKey = null;
    private String yKey = null;
    private double distance = 0.0;
    private Map<Integer, ValueMarker> opponents = new HashMap<>();

	public Profile() {
		super();
        // Dimension d
		// setPreferredSize(d);

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.PROFILE, this);
		MessageBus.INSTANCE.register(Messages.OPPONENTS, this);
	}

    private void handleClick(Point point) {
        Point2D p = chartPanel.translateScreenToJava2D(point);
        Rectangle2D plotArea = chartPanel.getScreenDataArea();
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
        double chartX = domainAxis.java2DToValue(p.getX(), plotArea, domainAxisEdge);
        if (chartX < 0.0) {
            chartX = 0.0;
        }
        double corr = 1.0;
        if (valCorrections.containsKey(xKey)) {
            corr = valCorrections.get(xKey);
        }
        MessageBus.INSTANCE.send(Messages.STARTPOS, chartX * corr);
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent cme) {
        handleClick(cme.getTrigger().getPoint());
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent cme) {
        // nothing
    }

    @Override
	public void callback(Messages message, Object o) {
        boolean rebuildSeries = false;
        switch (message) {
        case OPPONENTS:
            if (plot == null) {
                break;
            }
            OpponentData[] data = (OpponentData[]) o;
            Set<Integer> existing = new HashSet<>(opponents.keySet());
            if (data != null) {
                for (OpponentData opData : data) {
                    // "own" marker is displayed on telemetry
                    if (opData.getId() != 0) {
                        double dist = opData.getPassed();
                        if (valCorrections.containsKey(xKey)) {
                            dist /= valCorrections.get(xKey);
                        }
                        if (existing.contains(opData.getId())) {
                            existing.remove(opData.getId());
                            opponents.get(opData.getId()).setValue(dist);
                        } else {
                            ValueMarker oppMarker = new ValueMarker(dist);
                            oppMarker.setPaint(opData.getLabelColor());
                            BasicStroke stroke = new BasicStroke(1);
                            oppMarker.setStroke(stroke);
                            oppMarker.setLabel(opData.getLabel());
                            plot.addDomainMarker(oppMarker);
                            opponents.put(opData.getId(), oppMarker);
                        }
                    }
                }
            }
            for (Integer id : existing) {
                plot.removeDomainMarker(opponents.get(id));
                opponents.remove(id);
            }
            break;

        case TELEMETRY:
			Telemetry t = (Telemetry) o;
			distance = t.getDistance();
            if (valCorrections.containsKey(xKey)) {
                distance /= valCorrections.get(xKey);
            }
			break;

        case CLOSE:
            rebuildSeries = true;
            o = null;
            break;
        case GPXLOAD:
			RouteReader routeData = (RouteReader) o;
            rebuildSeries = true;
            o = routeData.getSeries();
            break;

        case PROFILE:
            rebuildSeries = true;
            break;
        }

        if (rebuildSeries) {
            XYSeries series = (XYSeries) o;
			if (chartPanel != null) {
				remove(chartPanel);
                chartPanel.removeChartMouseListener(this);
                chartPanel.revalidate();
                if ((plot != null) && (marker != null)) {
                    plot.removeDomainMarker(marker);
                }
                // remove all opponents
                if (!opponents.isEmpty()) {
                    callback(Messages.OPPONENTS, null);
                }
                plot = null;
                marker = null;
                chartPanel = null;
			}
            if (series == null) {
                UserPreferences.PROFILE_VISIBLE.setBool(false);
				return;
			}

            // get axis keys from series. No spaces are allowed
            xKey = "distance";
            yKey = "altitude";
            String key = (String) series.getKey();
            int index = key.indexOf(',');
            if (index >= 0) {
                xKey = key.substring(0, index);
                yKey = key.substring(index + 1);
            }

			// create the chart...
			XYDataset xyDataset = new XYSeriesCollection(series);
			final JFreeChart chart = ChartFactory.createXYAreaChart(
					null,
					MsgBundle.getString(xKey), // domain axis label
					MsgBundle.getString(yKey), // range axis label
					xyDataset, // data
					PlotOrientation.VERTICAL, // orientation
					false, // include legend
					false, // tooltips
					false // urls
					);

			chart.setBackgroundPaint(Color.darkGray);

			plot = chart.getXYPlot();
			// plot.setForegroundAlpha(0.85f);

            XYPlot plot = chart.getXYPlot();

            plot.setBackgroundPaint(Color.white);
			plot.setDomainGridlinePaint(Color.lightGray);
			plot.setRangeGridlinePaint(Color.lightGray);

			ValueAxis rangeAxis = plot.getRangeAxis();
			rangeAxis.setTickLabelPaint(Color.white);
			rangeAxis.setLabelPaint(Color.white);
			ValueAxis domainAxis = plot.getDomainAxis();
			domainAxis.setTickLabelPaint(Color.white);
			domainAxis.setLabelPaint(Color.white);

            if ("time_min".equals(xKey)) {
                ((NumberAxis) domainAxis).setNumberFormatOverride(timeFormat);
            }

            double minY = series.getMinY();
			double maxY = series.getMaxY();
            double delta = (maxY - minY) / 10.0;
            if ((long) minY  == 0) {
                rangeAxis.setRange(minY, maxY + delta);
            } else {
                rangeAxis.setRange(minY - delta, maxY + delta);
            }

			chartPanel = new ChartPanel(chart);
			// chartPanel.setSize(100, 800);

            // handle clicks to change training position
            chartPanel.addChartMouseListener(this);

			setLayout(new BorderLayout());
			add(chartPanel, BorderLayout.CENTER);
			setBackground(Color.black);
			chartPanel.revalidate();
            UserPreferences.PROFILE_VISIBLE.setBool(true);
		}

        if (plot != null) {
            if (marker != null) {
                marker.setValue(distance);
            } else {
                marker = new ValueMarker(distance);
                marker.setPaint(Color.blue);
                BasicStroke stroke = new BasicStroke(2);
                marker.setStroke(stroke);
                plot.addDomainMarker(marker);
            }
        }
	}
}
