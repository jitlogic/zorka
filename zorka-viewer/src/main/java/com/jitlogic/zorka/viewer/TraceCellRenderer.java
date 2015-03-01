/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Map;

public class TraceCellRenderer extends JLabel implements TableCellRenderer {

    public TraceCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        ViewerTraceRecord record = (ViewerTraceRecord)value;

        if (record != null) {
            if (isSelected) {
                setForeground(record.hasError() ? Color.RED : UIManager.getColor("Table.selectionForeground"));
                setBackground(UIManager.getColor("Table.selectionBackground"));
            } else if (hasFocus) {
                setForeground(record.hasError() ? Color.RED : UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            } else {
                setForeground(record.hasError() ? Color.RED : UIManager.getColor("Table.foreground"));
                setBackground(UIManager.getColor("Table.background"));
            }
            setText(record.getTraceLabel());
        }

        return this;
    }
}
