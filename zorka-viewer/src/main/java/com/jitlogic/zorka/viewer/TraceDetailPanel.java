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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class TraceDetailPanel extends JPanel {

    private TraceDataSet dataSet;

    private ErrorDetailView pnlStackTrace;

    /** This table lists method call tree of a selected trace. */
    private JTable tblTraceDetail;

    /** Table model for tblTraceDetail */
    private TraceDetailTableModel tbmTraceDetail;

    private JTextField txtSearch;

    private double minPct;
    private boolean errOnly;
    private Set<String> omits = new HashSet<String>();
    private JButton btnErrorFilterErrors;


    private class PctFilterAction extends AbstractAction {

        private double pct;

        public PctFilterAction(String icon, double pct) {
            super("", ResourceManager.getIcon16x16("filter-pct-"+icon));
            this.pct = pct;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            minPct = pct;
            tbmTraceDetail.filterOut(minPct, errOnly, omits);
        }
    }


    private class ErrFilterAction extends AbstractAction {

        public ErrFilterAction(String title, String icon) {
            super(title, ResourceManager.getIcon16x16(icon));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            errOnly = !errOnly;
            btnErrorFilterErrors.setSelected(errOnly);
            tbmTraceDetail.filterOut(minPct, errOnly, omits);
        }
    }


    private class SearchAction extends AbstractAction {

        public SearchAction(String title, String icon, boolean forward) {
            super(title, ResourceManager.getIcon16x16(icon));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Pattern pattern = Pattern.compile(txtSearch.getText().trim().length() > 0 ? ".*"+txtSearch.getText().trim()+".*" : ".*");

            for (int i = tblTraceDetail.getSelectedRow() + 1; i < tbmTraceDetail.getRowCount(); i++) {
                ViewerTraceRecord rec = tbmTraceDetail.getRecord(i);
                if (pattern.matcher(rec.getClassName()).matches() || pattern.matcher(rec.getMethodName()).matches()) {
                    tblTraceDetail.getSelectionModel().setSelectionInterval(i, i);
                    return;
                }
            }
        }
    }

    private class ClearFiltersAction extends AbstractAction {

        public ClearFiltersAction() {
            super("", ResourceManager.getIcon16x16("clear"));
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

        initToolbar();
        initTable();

        if (listener != null) {
            tblTraceDetail.addMouseListener(listener);
        }
    }

    private void initTable() {
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
                    TraceDetailPanel.this.pnlStackTrace.update(dataSet.getSymbols(),
                        tbmTraceDetail.getRecord(selectedRow));
                    ViewerTraceRecord record = tbmTraceDetail.getRecord(selectedRow);
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
    }

    private void initToolbar() {
        JToolBar tbDetailFilters = new JToolBar();
        tbDetailFilters.setFloatable(false);
        tbDetailFilters.setRollover(true);

        ButtonGroup grpPctFilterButtons = new ButtonGroup();

        JToggleButton btnPctFilterAll = new JToggleButton(new PctFilterAction("0", 0.0));
        btnPctFilterAll.setFocusable(false);
        btnPctFilterAll.setToolTipText("Show all method calls");
        grpPctFilterButtons.add(btnPctFilterAll);
        tbDetailFilters.add(btnPctFilterAll);

        JToggleButton btnPctFilter01 = new JToggleButton(new PctFilterAction("01", 0.1));
        btnPctFilter01.setFocusable(false);
        btnPctFilter01.setToolTipText("Show calls that took > 0.1% of trace execution time");
        grpPctFilterButtons.add(btnPctFilter01);
        tbDetailFilters.add(btnPctFilter01);

        JToggleButton btnPctFilter1 = new JToggleButton(new PctFilterAction("1", 1.0));
        btnPctFilter1.setFocusable(false);
        btnPctFilter1.setToolTipText("Show calls that took > 1% of trace execution time");
        grpPctFilterButtons.add(btnPctFilter1);
        tbDetailFilters.add(btnPctFilter1);

        JToggleButton btnPctFilter10 = new JToggleButton(new PctFilterAction("10", 10.0));
        btnPctFilter10.setFocusable(false);
        btnPctFilter10.setToolTipText("Show calls that took > 10% of trace execution time");
        grpPctFilterButtons.add(btnPctFilter10);
        tbDetailFilters.add(btnPctFilter10);

        tbDetailFilters.addSeparator();

        ButtonGroup grpErrFilterButtons = new ButtonGroup();

        btnErrorFilterErrors = new JButton(new ErrFilterAction("", "exception-thrown"));
        btnErrorFilterErrors.setToolTipText("Show only methods with exceptions thrown");
        btnErrorFilterErrors.setFocusable(false);
        tbDetailFilters.add(btnErrorFilterErrors);
        grpErrFilterButtons.add(btnErrorFilterErrors);

        JButton btnClearFilters = new JButton(new ClearFiltersAction());
        btnClearFilters.setToolTipText("Clear all exclusions (show all methods once again)");
        btnClearFilters.setFocusable(false);
        tbDetailFilters.add(btnClearFilters);

        tbDetailFilters.addSeparator();

        JButton btnSearchPrev = new JButton(new SearchAction("", "arrow-left-4", true));
        btnSearchPrev.setFocusable(false);
        btnSearchPrev.setToolTipText("Search previous occurence");
        tbDetailFilters.add(btnSearchPrev);

        JButton btnSearchNext = new JButton(new SearchAction("", "arrow-right-4", true));
        btnSearchNext.setFocusable(false);
        btnSearchNext.setToolTipText("Search previous occurence");
        tbDetailFilters.add(btnSearchNext);

        txtSearch = new JTextField(32);

        tbDetailFilters.add(txtSearch);
        this.add(tbDetailFilters, BorderLayout.NORTH);
    }

    private int getMethodCellOffset() {
        int offs = 0;
        for (int i = 0; i < TraceDetailTableModel.METHOD_COLUMN; i++) {
            offs += tblTraceDetail.getColumnModel().getColumn(i).getWidth();
        }
        return offs;
    }

    public void setTrace(TraceDataSet dataSet, ViewerTraceRecord record) {
        this.dataSet = dataSet;
        tbmTraceDetail.setTrace(record);
    }

}
