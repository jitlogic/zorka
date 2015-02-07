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

import java.awt.event.*;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class TracePanel extends JPanel {

    private TraceDetailPanel pnlTraceDetail;

    private TraceDataSet dataSet;

    private JTextField txtMinTime;

    private JComboBox cmbTraceType;

    private String traceLabel;
    private long minTraceTime;
    private boolean errorsOnly;

    /** This table lists loaded traces. */
    private JTable tblTraces;

    /** Table model for tblTraces */
    private TraceTableModel tbmTraces = new TraceTableModel();
    private JToggleButton btnFilterErrors;


    private class TraceFilter implements ViewerRecordFilter {
        @Override
        public boolean matches(ViewerTraceRecord record) {
            return record.getTime() >= minTraceTime
                && (!errorsOnly || record.getException() != null)
                && (traceLabel == null || traceLabel.equals(record.getTraceName()));
        }
        @Override public boolean recurse(ViewerTraceRecord record) {
            return false;
        }
    }


    private TraceFilter traceFilter = new TraceFilter();



    private class FilterByTimeAction extends AbstractAction {
        FilterByTimeAction() {
            super("", ResourceManager.getIcon16x16("filter"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String s = txtMinTime.getText();
            if (s != null && s.trim().length() > 0) {
                double d = Double.parseDouble(s) * 1000000000L;
                minTraceTime = (long)d;
            } else {
                minTraceTime = 0;
            }

            tbmTraces.setDataSet(dataSet, traceFilter);
        }
    }


    private class FilterByErrorAction extends AbstractAction {
        FilterByErrorAction() {
            super("", ResourceManager.getIcon16x16("error-mark"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            errorsOnly = !errorsOnly;
            btnFilterErrors.setSelected(errorsOnly);
            tbmTraces.setDataSet(dataSet, traceFilter);
        }
    }


    public TracePanel(TraceDetailPanel pnlTraceDetail) {
        this.setLayout(new BorderLayout(0,0));
        this.pnlTraceDetail = pnlTraceDetail;

        initToolbar();
        initTable();
    }


    private void initTable() {
        JScrollPane scrTraces = new JScrollPane();

        tblTraces = new JTable(tbmTraces);
        tbmTraces.adjustColumns(tblTraces);
        tblTraces.setAutoscrolls(false);

        tblTraces.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TracePanel.this.pnlTraceDetail.setTrace(dataSet, tbmTraces.get(tblTraces.getSelectedRow()));
            }
        });

        tblTraces.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        scrTraces.setMinimumSize(new Dimension(200, 384));
        scrTraces.setViewportView(tblTraces);

        add(scrTraces, BorderLayout.CENTER);
    }


    private void initToolbar() {
        JToolBar tbTraceFilters = new JToolBar();
        tbTraceFilters.setFloatable(false);
        tbTraceFilters.setRollover(true);

        tbTraceFilters.add(new JLabel("Min time:"));
        txtMinTime = new JTextField(4);
        tbTraceFilters.add(txtMinTime);

        JButton btnFilterByTime = new JButton(new FilterByTimeAction());
        btnFilterByTime.setFocusable(false);
        btnFilterByTime.setToolTipText("Filter by trace execution time");
        tbTraceFilters.add(btnFilterByTime);

        tbTraceFilters.addSeparator();

        btnFilterErrors = new JToggleButton(new FilterByErrorAction());
        btnFilterErrors.setFocusable(false);
        btnFilterErrors.setToolTipText("Show only traces with errors");
        tbTraceFilters.add(btnFilterErrors);

        tbTraceFilters.addSeparator();

        cmbTraceType = new JComboBox();
        cmbTraceType.addItem("*");

        cmbTraceType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String item = (String)e.getItem();
                    traceLabel = "*".equals(item) ? null : item;
                    tbmTraces.setDataSet(dataSet, traceFilter);
                }
            }
        });

        tbTraceFilters.add(cmbTraceType);

        add(tbTraceFilters, BorderLayout.NORTH);
    }


    public void setData(TraceDataSet dataSet) {
        this.dataSet = dataSet;
        this.errorsOnly = false;
        this.traceLabel = null;
        this.minTraceTime = 0;

        btnFilterErrors.setSelected(false);
        txtMinTime.setText("");

        tbmTraces.setDataSet(dataSet, null);

        List<String> traceNames = new ArrayList<String>();

        for (ViewerTraceRecord rec : dataSet.getRecords()) {
            String traceName = rec.getTraceName();
            if (traceName != null && !traceNames.contains(traceName)) {
                traceNames.add(traceName);
            }
        }

        Collections.sort(traceNames);
        cmbTraceType.removeAllItems();
        cmbTraceType.addItem("*");

        for (String traceName : traceNames) {
            cmbTraceType.addItem(traceName);
        }
    }


}
