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
package com.jitlogic.zorka.central.web.client;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.SingleSelectionModel;
import com.jitlogic.zorka.central.web.client.data.TraceDataService;
import com.jitlogic.zorka.central.web.client.data.TraceInfo;
import com.jitlogic.zorka.central.web.client.data.TraceRecordInfo;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Date;
import java.util.HashMap;


public class TraceListPanel extends Composite {

    interface TraceListPanelUiBinder extends UiBinder<Widget, TraceListPanel> {
    }

    private static TraceListPanelUiBinder ourUiBinder = GWT.create(TraceListPanelUiBinder.class);

    @UiField
    Button btnClose;

    @UiField
    DataGrid<TraceInfo> traceTable;

    @UiField
    SimplePager pager;

    private int hostId;
    private TraceDataService service;
    private ZorkaCentral central;

    private SingleSelectionModel<TraceInfo> sel = new SingleSelectionModel<TraceInfo>();
    private TraceListDataProvider dataProvider;


    public TraceListPanel(ZorkaCentral central, int hostId) {
        this.hostId = hostId;
        this.central = central;
        this.service = central.getTraceDataService();

        this.dataProvider = new TraceListDataProvider(service);

        initWidget(ourUiBinder.createAndBindUi(this));
        configureTraceTable();

        traceTable.setSelectionModel(sel);

        traceTable.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                TraceInfo info = sel.getSelectedObject();
                GWT.log("Selected trace: " + info.getHostId() + ":" + info.getDataOffs());
                openTraceDetailPanel(info);
            }
        }, DoubleClickEvent.getType());
    }


    private void openTraceDetailPanel(final TraceInfo info) {
        service.getTraceRecord(info.getHostId(), info.getDataOffs(), "",
                new MethodCallback<TraceRecordInfo>() {
                    @Override
                    public void onFailure(Method method, Throwable exception) {
                        GWT.log("Error calling method " + method, exception);
                    }

                    @Override
                    public void onSuccess(Method method, TraceRecordInfo record) {
                        TraceDetailPanel tdp = new TraceDetailPanel(service, info, record);
                        GWT.log("Root method: " + record);
                        central.add(tdp, "Details"); // TODO meaningful title
                    }
                });
    }


    private void configureTraceTable() {

        Column<TraceInfo, String> colTstamp = new Column<TraceInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceInfo info) {
                Date d = new Date(info.getClock());
                return DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(d)
                        + "." + NumberFormat.getFormat("000").format(info.getClock() % 1000);
            }
        };
        traceTable.addColumn(colTstamp, "Timestamp");
        traceTable.setColumnWidth(colTstamp, "125px");

        Column<TraceInfo, String> colDuration = new Column<TraceInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceInfo info) {
                double t = 1.0 * info.getExecutionTime() / 1000000.0;
                String u = "ms";

                if (t > 1000.0) {
                    t /= 1000.0;
                    u = "s";
                }

                return t > 10
                        ? NumberFormat.getFormat("#####").format(t) + u
                        : NumberFormat.getFormat("###.00").format(t) + u;
            }
        };
        traceTable.addColumn(colDuration, "Time");
        traceTable.setColumnWidth(colDuration, "75px");

        Column<TraceInfo, String> colCalls = new Column<TraceInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceInfo info) {
                return "" + info.getCalls();
            }
        };
        traceTable.addColumn(colCalls, "Calls");
        traceTable.setColumnWidth(colCalls, "75px");

        Column<TraceInfo, String> colErrors = new Column<TraceInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceInfo info) {
                return "" + info.getErrors();
            }
        };
        traceTable.addColumn(colErrors, "Errors");
        traceTable.setColumnWidth(colErrors, "75px");

        Column<TraceInfo, String> colRecords = new Column<TraceInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceInfo info) {
                return "" + info.getRecords();
            }
        };
        traceTable.addColumn(colRecords, "Recs");
        traceTable.setColumnWidth(colRecords, "75px");

        Column<TraceInfo, String> colDesc = new Column<TraceInfo, String>(new TextCell()) {
            @Override
            public String getValue(TraceInfo info) {
                return info.getDescription();
            }
        };
        traceTable.addColumn(colDesc, "Description");

        dataProvider.addDataDisplay(traceTable);
        dataProvider.setHostId(hostId);

        traceTable.setEmptyTableWidget(new Label("No traces"));

        pager.setDisplay(traceTable);
    }

}