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
import com.wattzap.model.RouteReader;
import com.wattzap.model.dto.Telemetry;

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
	ValueMarker marker = null;
	XYPlot plot;
	private ChartPanel chartPanel = null;

	private static Logger logger = LogManager.getLogger("Profile");

	public Profile(Dimension d) {
		super();

		// this.setPreferredSize(d);

		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.STARTPOS, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
	}

	@Override
	public void callback(Messages message, Object o) {
		double distance = 0.0;
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

			if (chartPanel != null) {
				remove(chartPanel);
                if (routeData.getSeries() == null) {
					setVisible(false);
					chartPanel.revalidate();
					return;
				}
			} else if (routeData.getSeries() == null) {
				return;
			}

			logger.debug("Load " + routeData.getPath());
			XYDataset xyDataset = new XYSeriesCollection(routeData.getSeries());

			// create the chart...
			final JFreeChart chart = ChartFactory.createXYAreaChart(
					// title
					routeData.getName(),
					MsgBundle.getString(routeData.getXKey()), // domain axis label
					MsgBundle.getString(routeData.getYKey()), // range axis label
					xyDataset, // data
					PlotOrientation.VERTICAL, // orientation
					false, // include legend
					false, // tooltips
					false // urls
					);

			chart.setBackgroundPaint(Color.darkGray);

			plot = chart.getXYPlot();
			// plot.setForegroundAlpha(0.85f);

			plot.setBackgroundPaint(Color.white);
			plot.setDomainGridlinePaint(Color.lightGray);
			plot.setRangeGridlinePaint(Color.lightGray);

			ValueAxis rangeAxis = plot.getRangeAxis();
			rangeAxis.setTickLabelPaint(Color.white);
			rangeAxis.setLabelPaint(Color.white);
			ValueAxis domainAxis = plot.getDomainAxis();
			domainAxis.setTickLabelPaint(Color.white);
			domainAxis.setLabelPaint(Color.white);

			double minY = routeData.getSeries().getMinY();
			double maxY = routeData.getSeries().getMaxY();
            double delta = (maxY - minY) / 10.0;
            if ((long) minY  == 0) {
                rangeAxis.setRange(minY, maxY + delta);
            } else {
                rangeAxis.setRange(minY - delta, maxY + delta);
            }

			chartPanel = new ChartPanel(chart);
			// chartPanel.setSize(100, 800);

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
            marker = new ValueMarker(distance);

            marker.setPaint(Color.blue);
            BasicStroke stroke = new BasicStroke(2);
            marker.setStroke(stroke);
            plot.addDomainMarker(marker);
        }
	}
}
