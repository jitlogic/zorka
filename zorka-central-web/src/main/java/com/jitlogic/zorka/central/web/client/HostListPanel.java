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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.ArrayList;
import java.util.List;


public class HostListPanel extends Composite {

    interface HostListPanelUiBinder extends UiBinder<Widget, HostListPanel> { }

    private static HostListPanelUiBinder ourUiBinder = GWT.create(HostListPanelUiBinder.class);

    @UiField DataGrid<RoofRecord> hostTable;

    private SingleSelectionModel<RoofRecord> sel = new SingleSelectionModel<RoofRecord>();

    private RoofClient<RoofRecord> client = new RoofClient<RoofRecord>("roof/hosts");

    private ZorkaCentral central;

    public HostListPanel(ZorkaCentral central) {
        this.central = central;
        initWidget(ourUiBinder.createAndBindUi(this));
        configureTable();
        loadHostList();
    }


    private void configureTable() {
        RoofDataColumnRenderers.textColumn(hostTable, "HOST_NAME", "Host name", "250px");
        RoofDataColumnRenderers.textColumn(hostTable, "HOST_ADDR", "Host address", "250px");

        hostTable.setSelectionModel(sel);

        hostTable.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                RoofRecord selected = sel.getSelectedObject();
                GWT.log("Selected host id: " + selected.getS("HOST_ID"));
                central.add(new TraceListPanel(central, selected.getS("HOST_ID")), selected.getS("HOST_NAME"));
            }
        }, DoubleClickEvent.getType());
    }




    private void loadHostList() {
        client.list(null, new AsyncCallback<JsArray<RoofRecord>>() {
            @Override
            public void onFailure(Throwable e) {
                GWT.log("Error fetching host list", e);
            }

            @Override
            public void onSuccess(JsArray<RoofRecord> result) {
                List<RoofRecord> hostList = new ArrayList<RoofRecord>();
                for (int i = 0; i < result.length(); i++) {
                    hostList.add(result.get(i));
                }
                hostTable.setPageSize(hostList.size());
                hostTable.setRowData(hostList);
                hostTable.setRowCount(hostList.size());
            }
        });
    }

}