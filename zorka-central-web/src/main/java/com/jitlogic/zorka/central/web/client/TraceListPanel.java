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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;


public class TraceListPanel extends Composite {

    interface TraceListPanelUiBinder extends UiBinder<Widget, TraceListPanel> { }

    private static TraceListPanelUiBinder ourUiBinder = GWT.create(TraceListPanelUiBinder.class);

    @UiField ListBox hostList;
    @UiField DataGrid<RoofRecord> traceTable;
    @UiField SimplePager pager;


    RoofClient<RoofRecord> client = new RoofClient<RoofRecord>("roof/hosts");
    RoofTableDataProvider traceListProvider = new RoofTableDataProvider(client);


    public TraceListPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        loadHostList();
        configureTraceTable();
    }

    private void loadHostList() {
        client.list(null, new AsyncCallback<JsArray<RoofRecord>>() {
            @Override
            public void onFailure(Throwable e) {
                GWT.log("Error fetching host list", e);
            }

            @Override
            public void onSuccess(JsArray<RoofRecord> result) {
                for (int i = 0; i < result.length(); i++) {
                    RoofRecord rec = result.get(i);
                    hostList.addItem(rec.getS("HOST_NAME"), ""+rec.getI("HOST_ID"));
                }
            }
        });
    }


    private void configureTraceTable() {
        RoofDataColumnRenderers.tstampColumn(traceTable, "CLOCK", "Timestamp", "125px");
        RoofDataColumnRenderers.durationColumn(traceTable, "EXTIME", "Time", "75px");
        RoofDataColumnRenderers.textColumn(traceTable, "CALLS", "Calls", "75px");
        RoofDataColumnRenderers.textColumn(traceTable, "ERRORS", "Errs", "75px");
        RoofDataColumnRenderers.textColumn(traceTable, "RECORDS", "Recs", "75px");
        RoofDataColumnRenderers.textColumn(traceTable, "OVERVIEW", "Description", null);

        traceListProvider.addDataDisplay(traceTable);
        traceTable.setEmptyTableWidget(new Label("No traces"));

        pager.setDisplay(traceTable);
    }


    @UiHandler("hostList")
    public void onHostChange(ChangeEvent e) {
        String hostIdStr = hostList.getValue(hostList.getSelectedIndex());
        GWT.log("Selecting host: " + hostIdStr);
        traceListProvider.setHostId(hostIdStr);
    }
}