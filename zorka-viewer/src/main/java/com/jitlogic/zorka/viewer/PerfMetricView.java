/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.viewer;

import com.jitlogic.zorka.common.Metric;
import com.jitlogic.zorka.common.PerfSample;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import java.text.SimpleDateFormat;
import java.util.List;

public class PerfMetricView extends JPanel {

    private JFreeChart chart;

    private TimeSeriesCollection dataset;

    public PerfMetricView() {
        setBorder(new EmptyBorder(3,3,3,3));
        setLayout(new BorderLayout(0, 0));

        dataset = new TimeSeriesCollection();

        chart = ChartFactory.createTimeSeriesChart("Performance: ", "Time", "", dataset, true, true, false);

        XYPlot plot = chart.getXYPlot();
        DateAxis axis = (DateAxis)plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss.SSS"));

        add(new ChartPanel(chart), BorderLayout.CENTER);
    }

    public void setData(String scanner, Metric metric, List<PerfSample> samples) {

        chart.setTitle("Performance: " + scanner);

        TimeSeries series = new TimeSeries(metric.getName());

        for (PerfSample sample : samples) {
            series.add(new FixedMillisecond(sample.getClock()), sample.getValue());
        }

        dataset.removeAllSeries();
        dataset.addSeries(series);


    }
}
