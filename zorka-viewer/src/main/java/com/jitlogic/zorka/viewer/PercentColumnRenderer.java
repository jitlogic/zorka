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
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PercentColumnRenderer extends JLabel implements TableCellRenderer {

    private static final Color BLACK = new Color(0, 0, 0);

    public PercentColumnRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        Double percent = (Double) value;

        if (value != null) {

            int g = 255 - (int) (percent * 2.49);
            if (g < 0) {
                g = 0;
            }
            if (g > 255) {
                g = 255;
            }
            Color bgColor = new Color(255, g, g);

            if (isSelected) {
                setForeground(bgColor);
                setBackground(UIManager.getColor("Table.selectionBackground"));
            } else if (hasFocus) {
                setForeground(bgColor);
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            } else {
                setForeground(BLACK);
                setBackground(bgColor);
            }

            setText(String.format(percent >= 10.0 ? "%.0f" : "%.2f", percent));
        }

        return this;
    }
}
