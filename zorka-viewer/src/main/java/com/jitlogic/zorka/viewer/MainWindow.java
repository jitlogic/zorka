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

import java.awt.*;
import javax.swing.*;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

public class MainWindow extends JFrame {

    private JPanel contentPane;
    private JTable tblTraces;
    private JTextArea txtTrace;

    private TraceSet traceSet = new TraceSet();

    private AbstractTableModel tbmTraces = new AbstractTableModel() {
        @Override public int getRowCount() {
            return traceSet.size();
        }

        @Override public int getColumnCount() {
            return 1;
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            NamedTraceElement el = traceSet.get(rowIndex);
            return el.getTraceName() + " " + el.getClockDt();
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
        createUI();
    }

    private void createUI() {
        setTitle("ZORKA viewer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 1024, 768);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5,5,5,5));
        contentPane.setLayout(new BorderLayout(0,0));
        setContentPane(contentPane);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        contentPane.add(toolbar, BorderLayout.NORTH);

        bindKey(KeyEvent.VK_F1,  actHelp);
        JButton btnHelp = new JButton(actHelp);
        toolbar.add(btnHelp);

        bindKey(KeyEvent.VK_F3, actOpen);
        JButton btnOpen = new JButton(actOpen);
        toolbar.add(btnOpen);

        bindKey(KeyEvent.VK_F10, actQuit);

        //JButton btnQuit = new JButton(actQuit);
        //btnQuit.setHorizontalAlignment(SwingConstants.RIGHT);
        //toolbar.add(btnQuit);

        // TODO trace type selection box here

        // TODO full-text search box here

        JSplitPane splitPane = new JSplitPane();
        contentPane.add(splitPane, BorderLayout.CENTER);

        JScrollPane scrTraces = new JScrollPane();
        splitPane.setLeftComponent(scrTraces);

        tblTraces = new JTable(tbmTraces);
        tblTraces.setTableHeader(null);

        scrTraces.setMinimumSize(new Dimension(200, 384));
        scrTraces.setViewportView(tblTraces);

        JScrollPane scrTraceDetail = new JScrollPane();

        txtTrace = new JTextArea();
        splitPane.setRightComponent(txtTrace);

        txtTrace.setMinimumSize(new Dimension(640, 384));

        splitPane.setResizeWeight(0.2);
    }


    private void bindKey(int key, Action action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key,0),action);
        getRootPane().getActionMap().put(action,action);
    }



}
