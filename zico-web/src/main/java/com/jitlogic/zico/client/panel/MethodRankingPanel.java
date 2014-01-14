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


import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.MethodRankInfoProxy;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;

import javax.inject.Inject;
import java.util.List;

public class MethodRankingPanel extends VerticalLayoutContainer {

    private ZicoRequestFactory rf;
    private TraceInfoProxy traceInfo;
    private ErrorHandler errorHandler;

    private DataGrid<MethodRankInfoProxy> rankGrid;
    private ListDataProvider<MethodRankInfoProxy> rankStore;
    private SingleSelectionModel<MethodRankInfoProxy> selectionModel;

    @Inject
    public MethodRankingPanel(ZicoRequestFactory rf, ErrorHandler errorHandler, @Assisted TraceInfoProxy traceInfo) {
        this.rf = rf;
        this.traceInfo = traceInfo;
        this.errorHandler = errorHandler;

        createRankingGrid();
        loadData("calls", "DESC");
    }


    private final static ProvidesKey<MethodRankInfoProxy> KEY_PROVIDER = new ProvidesKey<MethodRankInfoProxy>() {
        @Override
        public Object getKey(MethodRankInfoProxy item) {
            return item.getMethod();
        }
    };


    private void createRankingGrid() {

        rankGrid = new DataGrid<MethodRankInfoProxy>(1024*1024, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<MethodRankInfoProxy>(KEY_PROVIDER);
        rankGrid.setSelectionModel(selectionModel);

        Column<MethodRankInfoProxy,String> colMethod = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return m.getMethod();
            }
        };
        rankGrid.addColumn(colMethod, new ResizableHeader<MethodRankInfoProxy>("Method", rankGrid, colMethod));
        rankGrid.setColumnWidth(colMethod, 100, Style.Unit.PCT);

        Column<MethodRankInfoProxy,String> colCalls = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return "" + m.getCalls();
            }
        };
        rankGrid.addColumn(colCalls, new ResizableHeader<MethodRankInfoProxy>("Calls", rankGrid, colCalls));
        rankGrid.setColumnWidth(colCalls, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colErrors = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ""+m.getErrors();
            }
        };
        rankGrid.addColumn(colErrors, new ResizableHeader<MethodRankInfoProxy>("Errors", rankGrid, colErrors));
        rankGrid.setColumnWidth(colErrors, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getTime());
            }
        };
        rankGrid.addColumn(colErrors, new ResizableHeader<MethodRankInfoProxy>("Time", rankGrid, colTime));
        rankGrid.setColumnWidth(colTime, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colMinTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getMinTime());
            }
        };
        rankGrid.addColumn(colMinTime, new ResizableHeader<MethodRankInfoProxy>("MinT", rankGrid, colMinTime));
        rankGrid.setColumnWidth(colMinTime, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colMaxTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getMaxTime());
            }
        };
        rankGrid.addColumn(colMaxTime, new ResizableHeader<MethodRankInfoProxy>("MaxT", rankGrid, colMaxTime));
        rankGrid.setColumnWidth(colMaxTime, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colAvgTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getAvgTime());
            }
        };
        rankGrid.addColumn(colAvgTime, new ResizableHeader<MethodRankInfoProxy>("AvgT", rankGrid, colAvgTime));
        rankGrid.setColumnWidth(colAvgTime, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colBareTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getBareTime());
            }
        };
        rankGrid.addColumn(colBareTime, new ResizableHeader<MethodRankInfoProxy>("BT", rankGrid, colBareTime));
        rankGrid.setColumnWidth(colBareTime, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colMaxBareTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getMaxBareTime());
            }
        };
        rankGrid.addColumn(colMaxBareTime, new ResizableHeader<MethodRankInfoProxy>("MaxBT", rankGrid, colMaxBareTime));
        rankGrid.setColumnWidth(colMaxBareTime, 50, Style.Unit.PX);

        Column<MethodRankInfoProxy,String> colMinBareTime = new Column<MethodRankInfoProxy, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfoProxy m) {
                return ClientUtil.formatDuration(m.getMinBareTime());
            }
        };
        rankGrid.addColumn(colMinBareTime, new ResizableHeader<MethodRankInfoProxy>("MinBT", rankGrid, colMinBareTime));
        rankGrid.setColumnWidth(colMinBareTime, 50, Style.Unit.PX);

        rankStore = new ListDataProvider<MethodRankInfoProxy>(KEY_PROVIDER);
        rankStore.addDataDisplay(rankGrid);

        add(rankGrid, new VerticalLayoutData(1, 1));
    }


    private void loadData(String orderBy, String orderDir) {
        rf.traceDataService().traceMethodRank(traceInfo.getHostName(), traceInfo.getDataOffs(), orderBy, orderDir).fire(
                new Receiver<List<MethodRankInfoProxy>>() {
                    @Override
                    public void onSuccess(List<MethodRankInfoProxy> ranking) {
                        rankStore.getList().clear();
                        rankStore.getList().addAll(ranking);
                    }
                    @Override
                    public void onFailure(ServerFailure error) {
                        errorHandler.error("Error loading method rank data", error);
                    }
                }
        );
    }
}
