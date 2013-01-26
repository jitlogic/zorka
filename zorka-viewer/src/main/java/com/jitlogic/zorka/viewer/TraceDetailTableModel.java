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

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class TraceDetailTableModel extends AbstractTableModel {

    private String[] colNames = { "Time", "Pct", "Calls", "Err", "Method" };
    private int[] colWidth    = { 75, 75, 50, 50, 640 };

    private List<NamedTraceRecord> data = new ArrayList<NamedTraceRecord>(1);


    public void adjustColumns(JTable table) {
        for (int i = 0; i < colWidth.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }

        table.getColumnModel().getColumn(1).setCellRenderer(new PercentColumnRenderer());
        //table.getColumnModel().getColumn(4).setCellRenderer(new TraceMethodRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new MethodCellRenderer());
    }


    public void setRoot(NamedTraceRecord root) {
        data = new ArrayList<NamedTraceRecord>(root.getRecords()+2);
        root.scanRecords(data);
        fireTableDataChanged();
    }


    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }


    @Override
    public int getRowCount() {
        return data.size();
    }


    @Override
    public int getColumnCount() {
        return colNames.length;
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < data.size()) {
            NamedTraceRecord el = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return ViewerUtil.nanoSeconds(el.getTime());
                case 1:
                    return el.getTimePct();
                case 2:
                    return el.getCalls();
                case 3:
                    return el.getErrors();
                case 4:
                    return el;

            }
        }

        return "?";
    }


    public NamedTraceRecord getRecord(int idx) {
        return data.get(idx);
    }

}
