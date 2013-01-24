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
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TraceMethodRenderer extends JTextArea implements TableCellRenderer {

    public TraceMethodRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {

        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }

        setFont(table.getFont());

        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if (table.isCellEditable(row, column)) {
                setForeground(UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(new EmptyBorder(1,2,1,2));
        }

        NamedTraceRecord el = (NamedTraceRecord)value;

        setText(spc(el.getLevel()) + el.prettyPrint());

        setSize(new Dimension(table.getTableHeader().getColumnModel().getColumn(column).getWidth(), 1000));
        int prefh = getPreferredSize().height;

        if (table.getRowHeight(row) != prefh) {
            table.setRowHeight(row, prefh);
        }

        return this;
    }

    private static final String SPC = "    ";


    private String spc(int n) {
        StringBuilder sb = new StringBuilder(n * SPC.length() + 2);

        for (int i = 0; i < n; i++) {
            sb.append(SPC);
        }

        return sb.toString();
    }

}
