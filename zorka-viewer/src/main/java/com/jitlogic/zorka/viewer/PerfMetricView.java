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



//import info.monitorenter.gui.chart.Chart2D;
//import info.monitorenter.gui.chart.ITrace2D;
//import info.monitorenter.gui.chart.traces.Trace2DSimple;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PerfMetricView extends JPanel {

//    Chart2D chart;

//    private SimpleLinearGraph graph;

    //private JFreeChart chart;

    //private TimeSeriesCollection dataset;

    //private Map<Integer, ITrace2D>

    public PerfMetricView() {
        setBorder(new EmptyBorder(3,3,3,3));
        setLayout(new BorderLayout(0, 0));

//        chart = new Chart2D();
//        add(chart, BorderLayout.CENTER);
    }




    public void toggle(PerfMetricData pmd) {
//        ITrace2D trace = new Trace2DSimple();
//        chart.addTrace(trace);
//        trace.setColor(Color.RED);
//
//        for (int i = 0; i < pmd.size(); i++) {
//            trace.addPoint(pmd.getT(i), pmd.getV(i));
//        }
    }

}
