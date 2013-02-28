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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class TraceDetailPanel extends JPanel {

    private PerfDataSet traceSet;

    private ErrorDetailView pnlStackTrace;

    /** This table lists method call tree of a selected trace. */
    private JTable tblTraceDetail;

    /** Table model for tblTraceDetail */
    private TraceDetailTableModel tbmTraceDetail;

    private double minPct;
    private boolean errOnly;


    private class PctFilterAction extends AbstractAction {

        private double pct;

        public PctFilterAction(String title, double pct) {
            super(title);
            this.pct = pct;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            minPct = pct;
            tbmTraceDetail.filterOut(minPct, errOnly);
        }
    }

    private class ErrFilterAction extends AbstractAction {

        private boolean err;

        public ErrFilterAction(String title, boolean err) {
            super(title);
            this.err = err;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            errOnly = err;
            tbmTraceDetail.filterOut(minPct, errOnly);
        }
    }

    public TraceDetailPanel(ErrorDetailView pnlStackTrace, MouseListener listener) {
        this.pnlStackTrace = pnlStackTrace;

        this.setLayout(new BorderLayout(0,0));


        JToolBar tbDetailFilters = new JToolBar();
        tbDetailFilters.add(new JButton(new PctFilterAction("All", 0.0)));
        tbDetailFilters.add(new JButton(new PctFilterAction(">0.1%", 0.1)));
        tbDetailFilters.add(new JButton(new PctFilterAction(">1%", 1.0)));
        tbDetailFilters.add(new JButton(new PctFilterAction(">10%", 10.0)));
        tbDetailFilters.addSeparator();
        tbDetailFilters.add(new JButton(new ErrFilterAction("All", false)));
        tbDetailFilters.add(new JButton(new ErrFilterAction("Errors", true)));



        this.add(tbDetailFilters, BorderLayout.NORTH);


        JScrollPane scrTraceDetail = new JScrollPane();

        this.add(scrTraceDetail, BorderLayout.CENTER);

        tbmTraceDetail = new TraceDetailTableModel();
        tblTraceDetail = new JTable(tbmTraceDetail);
        tbmTraceDetail.configure(tblTraceDetail);
        tblTraceDetail.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblTraceDetail.setAutoCreateColumnsFromModel(false);
        scrTraceDetail.setViewportView(tblTraceDetail);

        tblTraceDetail.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TraceDetailPanel.this.pnlStackTrace.update(traceSet.getSymbols(),
                    tbmTraceDetail.getRecord(tblTraceDetail.getSelectedRow()));
            }
        });

        if (listener != null) {
            tblTraceDetail.addMouseListener(listener);
        }
    }

    public void setTrace(PerfDataSet traceSet, int i) {
        this.traceSet = traceSet;
        tbmTraceDetail.setTrace(traceSet.get(i));
    }

}
