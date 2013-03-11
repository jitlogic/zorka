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

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;

// TODO to be finished when other drill-down views will be complete.

public class SimpleLinearGraph extends JComponent {

    private int margin = 8;
    private String title = "";
    private long tstart, tstop;

    private List<PerfMetricData> metricData = new ArrayList<PerfMetricData>();

    @Override
    public void paint(Graphics g) {

        int x0 = margin,  y0 = margin, xw = getWidth() - 2 * margin, yh = getHeight() - 2 * margin;

        g.setColor(Color.GRAY);
        g.drawRect(x0-1, y0-1, xw+1, yh+1);

        g.setColor(Color.WHITE);
        g.fillRect(x0, y0, xw, yh);

    }


    public void setTitle(String title) {
        this.title = title;
    }


    public void setTimeRange(long tstart, long tstop) {
        this.tstart = tstart;
        this.tstop = tstop;
    }


    public void addMetricData(PerfMetricData pmd) {
        if (!metricData.contains(pmd)) {
            metricData.add(pmd);
        }
    }


    public void removeMetricData(PerfMetricData pmd) {
        metricData.remove(pmd);
    }
}
