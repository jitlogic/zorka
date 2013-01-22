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

import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.swing.tree.TreePath;

public class TraceDetailTreeModel extends AbstractTreeTableModel {

    private String[] colNames = { "Time", "Pct", "Calls", "Err", "Method" };
    private int[] colWidth    = { 75, 75, 50, 50, 640 };


    public void adjustColumns(JXTreeTable table) {
        for (int i = 0; i < colWidth.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }
    }


    public void setRoot(NamedTraceRecord root) {
        if (root != null) {
            NamedTraceRecord el = new NamedTraceRecord(null);
            el.addChild(root);
            el.setMethodName(root.getMethodName());
            el.setClassName(root.getClassName());
            el.setMethodSignature(root.getMethodSignature());
            el.setTime(el.getTime());
            this.root = el;
        } else {
            this.root = new JXFrame();
        }
        modelSupport.fireTreeStructureChanged(new TreePath(root));
    }


    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public int getHierarchicalColumn() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }


    @Override
    public Object getValueAt(Object o, int i) {

        if (o instanceof NamedTraceRecord) {
            NamedTraceRecord el = (NamedTraceRecord)o;
            switch (i) {
                case 0:
                    return ViewerUtil.nanoSeconds(el.getTime());
                case 1:
                    return String.format(el.getTimePct() >= 10.0 ? "%.0f" : "%.2f", el.getTimePct());
                case 2:
                    return el.getCalls();
                case 3:
                    return el.getErrors();
                case 4:
                    return el.prettyPrint();
            }
        }

        return "?";
    }


    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof NamedTraceRecord) {
            NamedTraceRecord el = (NamedTraceRecord)parent;
            return el.getChild(index);
        }

        return null;
    }


    @Override
    public int getChildCount(Object parent) {
        return parent instanceof NamedTraceRecord ? ((NamedTraceRecord)parent).numChildren() : 0;
    }


    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof NamedTraceRecord && child instanceof NamedTraceRecord) {
            NamedTraceRecord p = (NamedTraceRecord)parent;
            NamedTraceRecord c = (NamedTraceRecord)child;

            for (int i = 0; i < p.numChildren(); i++) {
                if (c.equals(p.getChild(i))) {
                    return i;
                }
            }
        }
        return -1;
    }
}
