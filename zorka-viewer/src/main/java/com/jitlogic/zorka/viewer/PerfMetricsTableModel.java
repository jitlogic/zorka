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

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerfMetricsTableModel extends AbstractTableModel {

    private static final String[] colNames = { "Scanner", "Metric" };
    private static final int[]    colWidth = { 50, 150 };

    private PerfDataSet dataSet;
    private List<PerfMetricData> performanceMetrics = new ArrayList<PerfMetricData>();


    /**
     * Configures supplied table. Sets colum names, preferred columns widths,
     * renders etc. Supplied table should use this model to present data.
     *
     * @param table table to be configure
     */
    public void configure(JTable table) {
        for (int i = 0; i < colWidth.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }
    }


    public void setData(PerfDataSet dataSet) {
        this.dataSet = dataSet;
        for (Map.Entry<Integer,PerfMetricData> e : dataSet.getMetricData().entrySet()) {
            performanceMetrics.add(e.getValue());
        }

        fireTableDataChanged();
    }


    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }


    @Override
    public int getRowCount() {
        return performanceMetrics.size();
    }


    @Override
    public int getColumnCount() {
        return colNames.length;
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return dataSet.getSymbol(performanceMetrics.get(rowIndex).getScannerId());
            case 1:
                return performanceMetrics.get(rowIndex).getMetric().getName();
        }

        return "?";
    }

    public void feed(PerfMetricView view, int idx) {
        view.toggle(performanceMetrics.get(idx));
    }
}
