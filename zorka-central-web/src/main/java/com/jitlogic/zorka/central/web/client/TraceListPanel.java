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
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.HashMap;


public class TraceListPanel extends Composite {

    interface TraceListPanelUiBinder extends UiBinder<Widget, TraceListPanel> { }

    private static TraceListPanelUiBinder ourUiBinder = GWT.create(TraceListPanelUiBinder.class);

    @UiField Button btnClose;
    @UiField DataGrid<RoofRecord> traceTable;
    @UiField SimplePager pager;

    private ZorkaCentral central;
    private String hostId;

    private RoofClient<RoofRecord> client = new RoofClient<RoofRecord>("roof/hosts");
    private RoofTableDataProvider traceListProvider = new RoofTableDataProvider(client);

    private SingleSelectionModel<RoofRecord> sel = new SingleSelectionModel<RoofRecord>();


    public TraceListPanel(ZorkaCentral central, final String hostId) {
        this.central = central;
        initWidget(ourUiBinder.createAndBindUi(this));
        configureTraceTable();
        this.hostId = hostId;
        traceListProvider.setHostId(hostId);

        traceTable.setSelectionModel(sel);

        traceTable.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                RoofRecord rec = sel.getSelectedObject();
                GWT.log("Selected trace: " + rec.getS("HOST_ID") + ":" + rec.getS("DATA_OFFS"));
                openTraceDetailPanel(rec);
            }
        }, DoubleClickEvent.getType());
    }

    private void openTraceDetailPanel(final RoofRecord rec) {
        client.callR(hostId + "/collections/traces/" + rec.getS("DATA_OFFS"), "getRecord", new HashMap<String, String>(),
                new AsyncCallback<RoofRecord>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log("Error fetching record: " + rec);
                    }

                    @Override
                    public void onSuccess(RoofRecord result) {
                        TraceDetailPanel tdp = new TraceDetailPanel(central, client, rec, result);
                        GWT.log("Root method: " + result.getS("METHOD"));
                        central.add(tdp, "Details");
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

}