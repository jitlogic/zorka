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
import java.util.Set;

/**
 * Table model for table showing all method calls in a trace.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceDetailTableModel extends AbstractTableModel {

    /** Table column names */
    private static final String[] colNames = { "Time", "Calls", "Err", "Pct", "Method" };

    /** Preferred table column widths */
    private static final int[] colWidth    = { 75, 65, 50, 50, 1640 };

    public static final int METHOD_COLUMN = 4;

    /** Current data ("flattened" method call tree representing single trace) */
    private List<NamedTraceRecord> data = new ArrayList<NamedTraceRecord>(1);

    private NamedTraceRecord root;

    private NamedRecordFilter filter;

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

        table.getColumnModel().getColumn(3).setCellRenderer(new PercentColumnRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new MethodCellRenderer());
    }


    public void filterOut(final double minPct, final boolean errOnly, final Set<String> omits) {
        filter = new NamedRecordFilter() {
            @Override
            public boolean matches(NamedTraceRecord record) {
                return record != null && record.getTimePct() >= minPct
                    && (!errOnly
                        || (0 != (record.getFlags() & NamedTraceRecord.EXCEPTION_PASS)
                        || record.getException() != null))
                    && !omits.contains(record.getClassName() + "." + record.getMethodName());
            }

            @Override
            public boolean recurse(NamedTraceRecord record) {
                return !omits.contains(record.getClassName() + "." + record.getMethodName());
            }
        };
        refresh();
    }


    /**
     * Sets trace root record for this table model. Triggers redraw od associated table.
     *
     * @param root new trace root record.
     */
    public void setTrace(NamedTraceRecord root) {
        this.root = root;
        refresh();
    }

    public void refresh() {
        if (root != null) {
            data = new ArrayList<NamedTraceRecord>(root.getRecords()+2);
            root.scanRecords(data, filter);
            fireTableDataChanged();
        }
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
                    return ZorkaUtil.strTime(el.getTime());
                case 1:
                    return el.getCalls();
                case 2:
                    return el.getErrors();
                case 3:
                    return el.getTimePct();
                case 4:
                    return el;

            }
        }

        return "?";
    }


    /**
     * Returns i-th trace record of this model. Record corresponds to i-th
     * item in associated table displaying trace.
     *
     * @param i trace record index
     *
     * @return trace record
     */
    public NamedTraceRecord getRecord(int i) {
        return data.get(i);
    }
}
