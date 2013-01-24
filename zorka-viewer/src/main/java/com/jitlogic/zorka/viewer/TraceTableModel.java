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

public class TraceTableModel extends AbstractTableModel {

    private String[] colNames = { "Date", "Time", "Calls", "Err", "Label" };
    private int[]    colWidth = { 75, 50, 50, 50, 150 };

    private TraceSet traceSet = new TraceSet();

    public void setTraceSet(TraceSet traceSet) {
        this.traceSet = traceSet;
        fireTableDataChanged();
    }

    public void adjustColumns(JTable table) {
        for (int i = 0; i < colWidth.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }
    }

    @Override
    public String getColumnName(int idx) {
        return colNames[idx];
    }

    @Override
    public int getRowCount() {
        return traceSet.size();
    }

    @Override
    public int getColumnCount() {
        return colWidth.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        NamedTraceRecord el = traceSet.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return el.prettyClock();
            case 1:
                return ViewerUtil.nanoSeconds(el.getTime());
            case 2:
                return el.getCalls();
            case 3:
                return el.getErrors();
            case 4:
                return el.getTraceName();
        }
        return "?";
    }
}
