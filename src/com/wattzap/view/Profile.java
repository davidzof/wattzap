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
import java.awt.Dimension;

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
import com.wattzap.model.dto.Telemetry;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Map;
import java.util.HashMap;
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
 * TODO add action listener to set new position on click, and remove slider.
 * TODO If x-axis key is time, there should be shown xx:xx instead of number of
 * seconds..
 */
public class Profile extends JPanel implements MessageCallback {
	private static Logger logger = LogManager.getLogger("Profile");
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


    private ValueMarker marker = null;
	private XYPlot plot = null;
	private ChartPanel chartPanel = null;
    private String xKey = null;
    private String yKey = null;
    private String routeName = null;
    private double distance = 0.0;

	public Profile(Dimension d) {
		super();
		// this.setPreferredSize(d);

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.STARTPOS, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.PROFILE, this);
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
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
			Telemetry t = (Telemetry) o;
			distance = t.getDistance();
			break;
		case STARTPOS:
			distance = (Double) o;
			break;

        case CLOSE:
			if (this.isVisible()) {
				remove(chartPanel);
				setVisible(false);
				revalidate();
			}
			return;
        case GPXLOAD:
			// Note if we are loading a Power Profile there is no GPX data so we don't show the chart panel
			RouteReader routeData = (RouteReader) o;
            routeName = routeData.getName();
			logger.debug("Load " + routeName);
            o = routeData.getSeries();
            /* no break: continue in PROFILE */

        case PROFILE:
            XYSeries series = (XYSeries) o;
			if (chartPanel != null) {
				remove(chartPanel);
                if (series == null) {
					setVisible(false);
					chartPanel.revalidate();
					return;
				}
			} else if (series == null) {
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
					// title
					routeName,
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
                final NumberFormat timeFormat =
                        new NumberFormat() {
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
            ChartMouseListener chartListener = new ChartMouseListener() {
                @Override
                public void chartMouseClicked(ChartMouseEvent cme) {
                    handleClick(cme.getTrigger().getPoint());
                }
                @Override
                public void chartMouseMoved(ChartMouseEvent cme) {
                    // nothing
                }
            };
            chartPanel.addChartMouseListener(chartListener);

			setLayout(new BorderLayout());
			add(chartPanel, BorderLayout.CENTER);
			setBackground(Color.black);
			chartPanel.revalidate();

			setVisible(true);
			break;
		}

        // marker must be recreated all the time?
        if (plot != null) {
            if (marker != null) {
                plot.removeDomainMarker(marker);
            }
            if (valCorrections.containsKey(xKey)) {
                marker = new ValueMarker(distance / valCorrections.get(xKey));
            } else {
                marker = new ValueMarker(distance);
            }

            marker.setPaint(Color.blue);
            BasicStroke stroke = new BasicStroke(2);
            marker.setStroke(stroke);
            plot.addDomainMarker(marker);
        }
	}
}
