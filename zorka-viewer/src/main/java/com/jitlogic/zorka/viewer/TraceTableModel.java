/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.core.util.ZorkaUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TraceTableModel extends AbstractTableModel {

    private String[] colNames = { "Date", "Time", "Calls", "Err", "Recs", "Label" };
    private int[]    colWidth = { 90, 50, 60, 40, 40, 550 };

    //private PerfDataSet traceSet = new PerfDataSet();
    private List<NamedTraceRecord> records = new ArrayList<NamedTraceRecord>();

    public void setTraceSet(PerfDataSet traceSet, NamedRecordFilter filter) {
        //this.traceSet = traceSet;
        if (traceSet != null) {
            records = new ArrayList<NamedTraceRecord>(traceSet.getTraces().size());

            for (NamedTraceRecord record : traceSet.getTraces()) {
                if (filter == null || filter.matches(record)) {
                    records.add(record);
                }
            }
        } else {
            records = new ArrayList<NamedTraceRecord>(1);
        }

        fireTableDataChanged();
    }

    public void adjustColumns(JTable table) {
        for (int i = 0; i < colWidth.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }
        table.getColumnModel().getColumn(5).setCellRenderer(new TraceCellRenderer());
    }

    @Override
    public String getColumnName(int idx) {
        return colNames[idx];
    }

    @Override
    public int getRowCount() {
        return records.size();
    }

    @Override
    public int getColumnCount() {
        return colWidth.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        NamedTraceRecord el = records.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return el.prettyClock();
            case 1:
                return ZorkaUtil.strTime(el.getTime());
            case 2:
                return el.getCalls();
            case 3:
                return el.getErrors();
            case 4:
                return el.getRecords();
            case 5:
                return el;
        }
        return "?";
    }



    public NamedTraceRecord get(int i) {
        return records.get(i);
    }
}
