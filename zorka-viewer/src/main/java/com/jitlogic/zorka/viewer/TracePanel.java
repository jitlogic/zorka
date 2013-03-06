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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TracePanel extends JPanel {

    private TraceDetailPanel pnlTraceDetail;

    private PerfDataSet traceSet;

    /** This table lists loaded traces. */
    private JTable tblTraces;

    /** Table model for tblTraces */
    private TraceTableModel tbmTraces = new TraceTableModel();


    public TracePanel(TraceDetailPanel pnlTraceDetail) {
        this.setLayout(new BorderLayout(0,0));
        this.pnlTraceDetail = pnlTraceDetail;

        JScrollPane scrTraces = new JScrollPane();

        tblTraces = new JTable(tbmTraces);
        tbmTraces.adjustColumns(tblTraces);

        tblTraces.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TracePanel.this.pnlTraceDetail.setTrace(traceSet, tblTraces.getSelectedRow());
            }
        });

        tblTraces.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        scrTraces.setMinimumSize(new Dimension(200, 384));
        scrTraces.setViewportView(tblTraces);

        add(scrTraces, BorderLayout.CENTER);
    }

    public void setData(PerfDataSet traceSet) {
        this.traceSet = traceSet;
        tbmTraces.setTraceSet(traceSet);
    }
}
