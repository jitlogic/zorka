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

import org.jdesktop.swingx.JXTreeTable;

import java.awt.*;
import javax.swing.*;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;

import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

public class MainWindow extends JFrame {

    private JPanel contentPane;

    private JTable tblTraces;
    private String[] colNames = { "Date", "Time", "Calls", "Err", "Label" };
    private int[]    colWidth = { 75, 50, 50, 50, 150 };


    private JXTreeTable tblTraceDetail;
    private TraceDetailTreeModel tbmTraceDetail;

    private TraceSet traceSet = new TraceSet();



    private AbstractTableModel tbmTraces = new AbstractTableModel() {

        @Override
        public String getColumnName(int idx) {
            return colNames[idx];
        }

        @Override
        public int getRowCount() {
            return traceSet.size();
        }

        @Override
        public int getColumnCount() {
            return colWidth.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            NamedTraceRecord el = traceSet.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return el.prettyClock();
                case 1:
                    return ViewerUtil.nanoSeconds(el.getTime());
                case 2:
                    return el.getCalls();
                case 3:
                    return el.getErrors();
                case 4:
                    return el.getTraceName();
            }
            return "?";
        }
    };


    private Action actHelp = new AbstractAction("Help [F1]",  ResourceManager.getIcon24x24("help")) {
        @Override public void actionPerformed(ActionEvent e) {

        }
    };


    private Action actOpen = new AbstractAction("Open [F3]", ResourceManager.getIcon24x24("open")) {
        @Override public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open trace file");
            int rv = chooser.showOpenDialog(contentPane);
            if (rv == JFileChooser.APPROVE_OPTION) {
                traceSet.load(chooser.getSelectedFile());
                tbmTraces.fireTableDataChanged();
            }
        }
    };


    private Action actQuit = new AbstractAction("Quit [F10]", ResourceManager.getIcon24x24("quit")) {
        @Override public void actionPerformed(ActionEvent e) {
            MainWindow.this.setVisible(false);
            System.exit(0);
        }
    };


    public MainWindow() {
        createMenuBar();
        createUI();
    }


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


    private void createUI() {
        setTitle("ZORKA viewer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 1024, 768);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5,5,5,5));
        contentPane.setLayout(new BorderLayout(0,0));
        setContentPane(contentPane);

        JSplitPane splitPane = new JSplitPane();
        contentPane.add(splitPane, BorderLayout.CENTER);

        JScrollPane scrTraces = new JScrollPane();
        splitPane.setLeftComponent(scrTraces);

        tblTraces = new JTable(tbmTraces);
        for (int i = 0; i < colWidth.length; i++) {
            tblTraces.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }

        tblTraces.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                displayTrace(tblTraces.getSelectedRow());
            }
        });

        scrTraces.setMinimumSize(new Dimension(200, 384));
        scrTraces.setViewportView(tblTraces);

        JScrollPane scrTraceDetail = new JScrollPane();
        splitPane.setRightComponent(scrTraceDetail);

        tbmTraceDetail = new TraceDetailTreeModel();
        tblTraceDetail = new JXTreeTable(tbmTraceDetail);
        tbmTraceDetail.adjustColumns(tblTraceDetail);
        tblTraceDetail.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tblTraceDetail.setAutoCreateColumnsFromModel(false);
        scrTraceDetail.setViewportView(tblTraceDetail);

        splitPane.setResizeWeight(0.2);
    }


    private void bindKey(int key, Action action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key,0),action);
        getRootPane().getActionMap().put(action,action);
    }


    private void displayTrace(int idx) {
        tbmTraceDetail.setRoot(traceSet.get(idx));
        //tbmTraceDetail.adjustColumns(tblTraceDetail);
        tblTraceDetail.expandAll();
    }

}
