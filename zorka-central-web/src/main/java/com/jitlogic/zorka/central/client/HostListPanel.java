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
package com.jitlogic.zorka.central.client;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;
import com.jitlogic.zorka.central.data.HostInfo;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.List;


public class HostListPanel extends Composite {

    interface HostListPanelUiBinder extends UiBinder<Widget, HostListPanel> {
    }

    private static HostListPanelUiBinder ourUiBinder = GWT.create(HostListPanelUiBinder.class);

    @UiField
    DataGrid<HostInfo> hostTable;

    private SingleSelectionModel<HostInfo> sel;
    private ZorkaCentral central;

    public HostListPanel(ZorkaCentral central) {
        this.central = central;
        initWidget(ourUiBinder.createAndBindUi(this));
        configureTable();
        loadHostList();
    }


    private void configureTable() {
        Column<HostInfo, String> colName = new Column<HostInfo, String>(new TextCell()) {
            @Override
            public String getValue(HostInfo object) {
                return object.getName();
            }
        };
        hostTable.addColumn(colName, "Host name");
        hostTable.setColumnWidth(colName, "250px");

        Column<HostInfo, String> colAddr = new Column<HostInfo, String>(new TextCell()) {
            @Override
            public String getValue(HostInfo object) {
                return object.getAddr();
            }
        };
        hostTable.addColumn(colAddr, "Host address");
        hostTable.setColumnWidth(colAddr, "250px");

        sel = new SingleSelectionModel<HostInfo>();
        hostTable.setSelectionModel(sel);

        hostTable.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                HostInfo info = sel.getSelectedObject();
                GWT.log("Selected host id: " + info.getId());
                central.add(new TraceListPanel(central, info.getId()), info.getName());
            }
        }, DoubleClickEvent.getType());
    }


    private void loadHostList() {
        central.getTraceDataService().listHosts(new MethodCallback<List<HostInfo>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling method " + method, exception);
            }

            @Override
            public void onSuccess(Method method, List<HostInfo> response) {
                hostTable.setPageSize(response.size());
                hostTable.setRowCount(response.size());
                hostTable.setRowData(response);
            }
        });
    }

}