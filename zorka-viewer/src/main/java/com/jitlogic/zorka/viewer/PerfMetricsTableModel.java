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

    private List<List<PerfSample>> series = new ArrayList<List<PerfSample>>();
    private List<Metric> metrics = new ArrayList<Metric>();
    private List<String> scanners = new ArrayList<String>();


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


    public void setData(PerfDataSet dataset) {
        series.clear();
        metrics.clear();
        scanners.clear();

        for (Map.Entry<Integer,Map<Integer,List<PerfSample>>> e1 : dataset.getMdata().entrySet()) {
            for (Map.Entry<Integer,List<PerfSample>> e2 : e1.getValue().entrySet()) {
                series.add(e2.getValue());
                metrics.add(dataset.getMetric(e2.getKey()));
                scanners.add(dataset.getSymbol(e1.getKey()));
            }
        }
        fireTableDataChanged();
    }


    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }


    @Override
    public int getRowCount() {
        return series.size();
    }


    @Override
    public int getColumnCount() {
        return colNames.length;
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return scanners.get(rowIndex);
            case 1:
                return metrics.get(rowIndex).getName();
        }

        return "?";
    }

    public void feed(PerfMetricView view, int idx) {
        view.setData(scanners.get(idx), metrics.get(idx), series.get(idx));
    }
}
