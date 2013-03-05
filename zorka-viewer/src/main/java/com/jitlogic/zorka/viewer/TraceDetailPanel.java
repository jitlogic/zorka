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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class TraceDetailPanel extends JPanel {

    private PerfDataSet traceSet;

    private ErrorDetailView pnlStackTrace;

    /** This table lists method call tree of a selected trace. */
    private JTable tblTraceDetail;

    /** Table model for tblTraceDetail */
    private TraceDetailTableModel tbmTraceDetail;

    private JTextField txtSearch;

    private double minPct;
    private boolean errOnly;
    private Set<String> omits = new HashSet<String>();


    private class PctFilterAction extends AbstractAction {

        private double pct;

        public PctFilterAction(String title, double pct) {
            super(title);
            this.pct = pct;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            minPct = pct;
            tbmTraceDetail.filterOut(minPct, errOnly, omits);
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
            tbmTraceDetail.filterOut(minPct, errOnly, omits);
        }
    }


    private class SearchAction extends AbstractAction {

        public SearchAction(String title, boolean forward) {
            super(title);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Pattern pattern = Pattern.compile(txtSearch.getText().trim().length() > 0 ? ".*"+txtSearch.getText().trim()+".*" : ".*");

            for (int i = tblTraceDetail.getSelectedRow() + 1; i < tbmTraceDetail.getRowCount(); i++) {
                NamedTraceRecord rec = tbmTraceDetail.getRecord(i);
                if (pattern.matcher(rec.getClassName()).matches() || pattern.matcher(rec.getMethodName()).matches()) {
                    tblTraceDetail.getSelectionModel().setSelectionInterval(i, i);
                    return;
                }
            }
        }
    }

    private class ClearFiltersAction extends AbstractAction {

        public ClearFiltersAction() {
            super("Clear");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            omits.clear();
            tbmTraceDetail.filterOut(minPct, errOnly, omits);
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
        tbDetailFilters.addSeparator();
        tbDetailFilters.add(new JButton(new ClearFiltersAction()));
        tbDetailFilters.addSeparator();

        txtSearch = new JTextField(32);
        //txtSearch.setSize(150, 16);
        //txtSearch.setMinimumSize(new Dimension(150, 16));

        tbDetailFilters.add(new JButton(new SearchAction("Search", true)));
        tbDetailFilters.add(new JButton(new SearchAction("Prev", false)));
        tbDetailFilters.add(txtSearch);
        this.add(tbDetailFilters, BorderLayout.NORTH);

        JScrollPane scrTraceDetail = new JScrollPane();
        this.add(scrTraceDetail, BorderLayout.CENTER);

        tbmTraceDetail = new TraceDetailTableModel();
        tblTraceDetail = new JTable(tbmTraceDetail);
        tbmTraceDetail.configure(tblTraceDetail);
        tblTraceDetail.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblTraceDetail.setAutoCreateColumnsFromModel(false);
        tblTraceDetail.setAutoscrolls(false);
        scrTraceDetail.setViewportView(tblTraceDetail);

        tblTraceDetail.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int selectedRow = tblTraceDetail.getSelectedRow();
                if (selectedRow >= 0) {
                    TraceDetailPanel.this.pnlStackTrace.update(traceSet.getSymbols(),
                        tbmTraceDetail.getRecord(selectedRow));
                    NamedTraceRecord record = tbmTraceDetail.getRecord(selectedRow);
                    switch (e.getButton()) {
                        case MouseEvent.BUTTON1: {
                            int x = e.getX()  - getMethodCellOffset();
                            int refX = record.getLevel() * MethodCellRenderer.SINGLE_LEVEL;
                            if (x >= refX && x <= refX+16) {
                                record.toggleExpanded();
                                tbmTraceDetail.refresh();
                            }
                        }
                        break;
                        case MouseEvent.BUTTON2: {
                            omits.add(record.getClassName() + "." + record.getMethodName());
                            tbmTraceDetail.filterOut(minPct, errOnly, omits);
                        }
                        break;
                        case MouseEvent.BUTTON3: {
                            System.out.println("TODO implement popup menu");
                        }
                        break;
                    }
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                int r = tblTraceDetail.rowAtPoint(e.getPoint());
                if (r >= 0 && r < tblTraceDetail.getRowCount()) {
                    tblTraceDetail.setRowSelectionInterval(r, r);
                } else {
                    tblTraceDetail.clearSelection();
                }
            }
        });

        if (listener != null) {
            tblTraceDetail.addMouseListener(listener);
        }
    }

    private int getMethodCellOffset() {
        int offs = 0;
        for (int i = 0; i < TraceDetailTableModel.METHOD_COLUMN; i++) {
            offs += tblTraceDetail.getColumnModel().getColumn(i).getWidth();
        }
        return offs;
    }

    public void setTrace(PerfDataSet traceSet, int i) {
        this.traceSet = traceSet;
        tbmTraceDetail.setTrace(traceSet.get(i));
    }

}
