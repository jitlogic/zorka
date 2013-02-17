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

import java.awt.*;
import javax.swing.*;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.border.EmptyBorder;

/**
 * Zorka Viewer main window.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MainWindow extends JFrame {

    /** Contains loaded traces */
    private PerfDataSet traceSet = new PerfDataSet();

    /** Content pane */
    private JPanel contentPane;

    private JTabbedPane tabTraces;

    /** This table lists loaded traces. */
    private JTable tblTraces;

    /** Table model for tblTraces */
    private TraceTableModel tbmTraces = new TraceTableModel();

    private JTable tblMetrics;

    private PerfMetricsTableModel tbmMetrics = new PerfMetricsTableModel();

    /** Tabbed pane containing various views depicting trace details. */
    private JTabbedPane tabDetail;

    /** This table lists method call tree of a selected trace. */
    private JTable tblTraceDetail;

    /** Table model for tblTraceDetail */
    private TraceDetailTableModel tbmTraceDetail;

    /** This view contains stack trace of currently selected method call trace record */
    private ErrorDetailView pnlStackTrace;

    /** Performance metric panel */
    private PerfMetricView pnlPerfMetric;

    /** Help action: displays help window. */
    private Action actHelp = new AbstractAction("Help [F1]",  ResourceManager.getIcon16x16("help")) {
        @Override public void actionPerformed(ActionEvent e) {

        }
    };

    /** Open action: opens file chooser dialog and loads trace file (if user selects and chooses to open it) */
    private Action actOpen = new AbstractAction("Open [F3]", ResourceManager.getIcon16x16("file-open")) {
        @Override public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open trace file");
            int rv = chooser.showOpenDialog(contentPane);
            if (rv == JFileChooser.APPROVE_OPTION) {
                traceSet.load(chooser.getSelectedFile());
                tbmTraces.setTraceSet(traceSet);
                tbmMetrics.setData(traceSet);
            }
        }
    };

    /** Quit action: closes viewer */
    private Action actQuit = new AbstractAction("Quit [F3]", ResourceManager.getIcon16x16("file-quit")) {
        @Override public void actionPerformed(ActionEvent e) {
            MainWindow.this.setVisible(false);
            System.exit(0);
        }
    };
    private JSplitPane splitPane;


    /**
     * Creates main window.
     */
    public MainWindow() {
        createMenuBar();
        createUI();
    }


    /**
     * Creates menu bar.
     */
    private void createMenuBar() {
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(actOpen));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(actQuit));
        bar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem(actHelp));
        helpMenu.addSeparator();
        helpMenu.add(new JMenuItem("About..."));
        bar.add(helpMenu);

        bindKey(KeyEvent.VK_F1,  actHelp);
        bindKey(KeyEvent.VK_F3, actOpen);
        bindKey(KeyEvent.VK_F10, actQuit);
    }


    /**
     * Creates all widgets of main window (except for menu bar)
     */
    private void createUI() {
        setTitle("ZORKA viewer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 1024, 768);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5,5,5,5));
        contentPane.setLayout(new BorderLayout(0,0));
        setContentPane(contentPane);


        splitPane = new JSplitPane();
        contentPane.add(splitPane, BorderLayout.CENTER);

        createLeftPanel();

        createDetailUI();

        splitPane.setResizeWeight(0.2);
    }


    private void createLeftPanel() {
        tabTraces = new JTabbedPane();
        splitPane.setLeftComponent(tabTraces);

        JScrollPane scrTraces = new JScrollPane();
        tabTraces.add("Traces", scrTraces);

        tblTraces = new JTable(tbmTraces);
        tbmTraces.adjustColumns(tblTraces);

        tblTraces.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                displayTrace(tblTraces.getSelectedRow());
            }
        });

        tblTraces.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        scrTraces.setMinimumSize(new Dimension(200, 384));
        scrTraces.setViewportView(tblTraces);


        JScrollPane scrMetrics = new JScrollPane();
        tabTraces.add("Metrics", scrMetrics);

        tblMetrics = new JTable(tbmMetrics);
        tbmMetrics.configure(tblMetrics);

        tblMetrics.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                displayMetric(tblMetrics.getSelectedRow());
            }
        });

        scrMetrics.setViewportView(tblMetrics);
    }


    private void createDetailUI() {
        JScrollPane scrTraceDetail = new JScrollPane();

        tbmTraceDetail = new TraceDetailTableModel();
        tblTraceDetail = new JTable(tbmTraceDetail);
        tbmTraceDetail.configure(tblTraceDetail);
        tblTraceDetail.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblTraceDetail.setAutoCreateColumnsFromModel(false);
        scrTraceDetail.setViewportView(tblTraceDetail);

        tblTraceDetail.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                displayMethod(tblTraceDetail.getSelectedRow(), e.getClickCount() > 1);
            }
        });

        tabDetail = new JTabbedPane();
        tabDetail.addTab("Trace details", scrTraceDetail);

        pnlStackTrace = new ErrorDetailView();
        tabDetail.addTab("Call details", pnlStackTrace);

        pnlPerfMetric = new PerfMetricView();
        tabDetail.addTab("Performance data", pnlPerfMetric);

        splitPane.setRightComponent(tabDetail);
    }


    /**
     * Binds a key to an action
     *
     * @param key key code
     *
     * @param action action
     */
    private void bindKey(int key, Action action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key,0),action);
        getRootPane().getActionMap().put(action,action);
    }


    /**
     * This method is called when user selects trace from trace table.
     * Updates trace detail widgets.
     *
     * @param idx trace index (in traces table)
     */
    private void displayTrace(int idx) {
        NamedTraceRecord root = traceSet.get(idx);
        tbmTraceDetail.setTrace(root);
    }


    /**
     * This method is called when user double clicks on a method in trace detail tree.
     *
     * @param idx method index (in trace detail method table)
     *
     * @param sw whether to switch to method/error detail tab
     */
    private void displayMethod(int idx, boolean sw) {
        pnlStackTrace.update(traceSet.getSymbols(), tbmTraceDetail.getRecord(idx));
        if (sw) {
            tabDetail.setSelectedComponent(pnlStackTrace);
        }
    }

    private void displayMetric(int idx) {
        tbmMetrics.feed(pnlPerfMetric, idx);
    }
}
