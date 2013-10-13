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
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.api.TraceDataApi;
import com.jitlogic.zico.data.SymbolicExceptionInfo;
import com.jitlogic.zico.data.TraceRecordInfo;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.container.BorderLayoutContainer;
import com.sencha.gxt.widget.core.client.container.SimpleContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellClickEvent;
import com.sencha.gxt.widget.core.client.form.TextArea;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.*;

public class MethodAttrsDialog extends Dialog {

    private Grid<String[]> attrGrid;
    private ListStore<String[]> attrStore;

    private TextArea txtAttrVal;
    private Label lblAttrName;

    private BorderLayoutContainer container;

    private TraceDataApi api;
    private ErrorHandler errorHandler;


    @Inject
    public MethodAttrsDialog(TraceDataApi api, ErrorHandler errorHandler,
                             @Assisted Integer hostId, @Assisted Long dataOffs,
                             @Assisted String path, @Assisted("minTime") Long minTime) {
        this.api = api;
        this.errorHandler = errorHandler;
        configure("Trace Details");

        loadTraceDetail(hostId, dataOffs, path, minTime);
    }


    private void loadTraceDetail(Integer hostId, Long dataOffs, String path, Long minTime) {
        api.getTraceRecord(hostId, dataOffs, minTime, path,
                new MethodCallback<TraceRecordInfo>() {
                    @Override
                    public void onFailure(Method method, Throwable exception) {
                        errorHandler.error("Error calling method: " + method, exception);
                        txtAttrVal.setText("Error calling method: " + method + "\n" + exception);
                    }

                    @Override
                    public void onSuccess(Method method, TraceRecordInfo tr) {
                        fillTraceDetail(tr);
                    }
                });
    }


    private void fillTraceDetail(TraceRecordInfo tr) {
        List<String[]> attrs = new ArrayList<String[]>();

        StringBuilder sb = new StringBuilder();

        if (tr.getAttributes() != null) {
            for (Map.Entry<String, String> e : tr.getAttributes().entrySet()) {
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

        if (tr.getExceptionInfo() != null) {
            SymbolicExceptionInfo e = tr.getExceptionInfo();
            sb = new StringBuilder();
            sb.append(e.getExClass() + ": " + e.getMessage() + "\n");
            for (String s : e.getStackTrace()) {
                sb.append(s + "\n");
            }
            attrs.add(new String[]{"(exception)", sb.toString()});
        }

        if (attrs.size() > 0) {
            attrStore.addAll(attrs);
            lblAttrName.setText("Selected attribute: " + attrs.get(0)[0]);
            txtAttrVal.setText(attrs.get(0)[1]);
        } else {
            txtAttrVal.setText("This method has no attributes and hasn't thrown any exception.");
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

        container = new BorderLayoutContainer();
        BorderLayoutContainer.BorderLayoutData westData = new BorderLayoutContainer.BorderLayoutData(256);

        westData.setMargins(new Margins(5, 0, 5, 5));
        westData.setSplit(true);
        westData.setCollapsible(true);
        westData.setCollapseHidden(true);
        westData.setCollapseMini(true);


        ContentPanel westContainer = new ContentPanel();
        westContainer.setHeaderVisible(false);
        westContainer.setBodyBorder(true);
        westContainer.add(attrGrid);
        westContainer.setWidth(200);

        container.setWestWidget(westContainer);

        txtAttrVal = new TextArea();
        txtAttrVal.setReadOnly(true);
        txtAttrVal.setText("Please wait ...");

        VerticalLayoutContainer vp = new VerticalLayoutContainer();
        lblAttrName = new Label("Selected attribute:");
        vp.add(lblAttrName);
        vp.add(txtAttrVal, new VerticalLayoutContainer.VerticalLayoutData(1, 1));

        SimpleContainer center = new SimpleContainer();
        center.add(vp);

        container.setCenterWidget(center);

        add(container);
    }
}
