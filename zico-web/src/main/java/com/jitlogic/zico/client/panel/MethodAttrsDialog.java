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
package com.jitlogic.zico.client.panel;


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.KeyValueProxy;
import com.jitlogic.zico.shared.data.SymbolicExceptionProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.container.SimpleContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellClickEvent;
import com.sencha.gxt.widget.core.client.form.TextArea;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MethodAttrsDialog extends Dialog {

    private Grid<String[]> attrGrid;
    private ListStore<String[]> attrStore;

    private TextArea txtAttrVal;
    private Label lblAttrName;

    private SplitLayoutPanel container;

    private ZicoRequestFactory rf;
    private ErrorHandler errorHandler;


    @Inject
    public MethodAttrsDialog(ZicoRequestFactory rf, ErrorHandler errorHandler,
                             @Assisted("hostName") String hostName, @Assisted Long dataOffs,
                             @Assisted String path, @Assisted("minTime") Long minTime) {
        this.rf = rf;
        this.errorHandler = errorHandler;
        configure("Trace Details");

        loadTraceDetail(hostName, dataOffs, path, minTime);
    }


    private void loadTraceDetail(String hostName, Long dataOffs, String path, Long minTime) {
        rf.traceDataService().getRecord(hostName, dataOffs, minTime, path).fire(new Receiver<TraceRecordProxy>() {
            @Override
            public void onSuccess(TraceRecordProxy tr) {
                fillTraceDetail(tr);
            }
        });
    }


    private void fillTraceDetail(TraceRecordProxy tr) {
        List<String[]> attrs = new ArrayList<String[]>();

        StringBuilder sb = new StringBuilder();

        sb.append(tr.getMethod() + "\n\n");

        if (tr.getAttributes() != null) {
            for (KeyValueProxy e : tr.getAttributes()) {
                String key = e.getKey(), val = e.getValue() != null ? e.getValue() : "";
                attrs.add(new String[]{key, val});
                val = val.indexOf("\n") != -1 ? val.substring(0, val.indexOf('\n')) + "..." : val;
                if (val.length() > 80) {
                    val = val.substring(0, 80) + "...";
                }
                sb.append(key + "=" + val + "\n");
            }
        }

        Collections.sort(attrs, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[0].compareTo(o2[0]);
            }
        });

        if (attrs.size() > 0) {
            attrs.add(0, new String[]{"(all)", sb.toString()});
        }

        SymbolicExceptionProxy e = tr.getExceptionInfo();
        sb = new StringBuilder();
        while (e != null) {
            sb.append(e.getExClass() + ": " + e.getMessage() + "\n");
            for (String s : e.getStackTrace()) {
                sb.append(s + "\n");
            }

            e = e.getCause();

            if (e != null) {
                sb.append("\nCaused by: ");
            }
        }

        if (sb.length() > 0) {
            attrs.add(new String[]{"(exception)", sb.toString()});
        }

        if (attrs.size() > 0) {
            attrStore.addAll(attrs);
            lblAttrName.setText("Selected attribute: " + attrs.get(0)[0]);
            txtAttrVal.setText(attrs.get(0)[1]);
        } else {
            txtAttrVal.setText(tr.getMethod() + "\n\n" + "This method has no attributes and hasn't thrown any exception.");
        }
    }


    private void configure(String headingText) {
        setHeadingText(headingText);
        setPredefinedButtons();
        setPixelSize(1200, 750);

        ValueProvider<String[], String> vpr = new ValueProvider<String[], String>() {
            @Override
            public String getValue(String[] object) {
                return object[0];
            }

            @Override
            public void setValue(String[] object, String value) {
                object[0] = value;
            }

            @Override
            public String getPath() {
                return "";
            }
        };


        ColumnConfig<String[], String> colAttribute = new ColumnConfig<String[], String>(vpr, 256, "Attributes");
        colAttribute.setMenuDisabled(true);


        colAttribute.setCell(new AbstractCell<String>() {
            @Override
            public void render(Context context, String value, SafeHtmlBuilder sb) {
                String color = "blue";
                if ("(all)".equals(value)) {
                    color = "black";
                }
                if ("(exception)".equals(value)) {
                    color = "red";
                }
                sb.appendHtmlConstant("<span style=\"color: " + color + "; font-size: small;\"><b>");
                sb.append(SafeHtmlUtils.fromString("" + value));
                sb.appendHtmlConstant("</b></span>");
            }
        });

        ColumnModel<String[]> columnModel = new ColumnModel<String[]>(Arrays.<ColumnConfig<String[], ?>>asList(colAttribute));

        attrStore = new ListStore<String[]>(new ModelKeyProvider<String[]>() {
            @Override
            public String getKey(String[] item) {
                return item[0];
            }
        });

        attrGrid = new Grid<String[]>(attrStore, columnModel);

        attrGrid.addCellClickHandler(new CellClickEvent.CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                String[] item = attrStore.get(event.getRowIndex());
                lblAttrName.setText("Selected attribute: " + item[0]);
                txtAttrVal.setText(item[1]);
            }
        });

        attrGrid.getView().setAutoExpandColumn(colAttribute);
        attrGrid.getView().setForceFit(true);

        container = new SplitLayoutPanel();

        SimpleContainer sc = new SimpleContainer();
        sc.add(attrGrid);

        container.addWest(sc, 200);

        txtAttrVal = new TextArea();
        txtAttrVal.setReadOnly(true);
        txtAttrVal.setText("Please wait ...");

        VerticalLayoutContainer vp = new VerticalLayoutContainer();
        lblAttrName = new Label("Selected attribute:");
        vp.add(lblAttrName);
        vp.add(txtAttrVal, new VerticalLayoutContainer.VerticalLayoutData(1, 1));

        container.add(vp);

        add(container);
    }
}
