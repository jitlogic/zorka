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


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.jitlogic.zorka.central.data.HostInfo;
import com.jitlogic.zorka.central.data.PagingData;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceInfoProperties;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.loader.*;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.grid.*;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TraceListingPanel extends VerticalLayoutContainer {

    private static final TraceInfoProperties props = GWT.create(TraceInfoProperties.class);

    private TraceDataService tds;

    private HostInfo selectedHost;
    private Grid<TraceInfo> traceGrid;
    private ListStore<TraceInfo> traceStore;
    private DataProxy<PagingLoadConfig, PagingLoadResult<TraceInfo>> traceProxy;
    private PagingLoader<PagingLoadConfig, PagingLoadResult<TraceInfo>> traceLoader;
    private LiveGridView<TraceInfo> traceGridView;

    private ZorkaCentralShell shell;

    public TraceListingPanel(ZorkaCentralShell shell, TraceDataService tds, HostInfo hostInfo) {
        this.shell = shell;
        this.tds = tds;
        this.selectedHost = hostInfo;

        createTraceListGrid();
    }


    private void createTraceListGrid() {
        ColumnConfig<TraceInfo, Long> clockCol = new ColumnConfig<TraceInfo, Long>(props.clock(), 100, "Clock");
        ColumnConfig<TraceInfo, Long> durationCol = new ColumnConfig<TraceInfo, Long>(props.executionTime(), 50, "Time");
        ColumnConfig<TraceInfo, Long> callsCol = new ColumnConfig<TraceInfo, Long>(props.calls(), 50, "Calls");
        ColumnConfig<TraceInfo, Long> errorsCol = new ColumnConfig<TraceInfo, Long>(props.errors(), 50, "Errors");
        ColumnConfig<TraceInfo, Long> recordsCol = new ColumnConfig<TraceInfo, Long>(props.records(), 50, "Records");
        ColumnConfig<TraceInfo, String> descCol = new ColumnConfig<TraceInfo, String>(props.description(), 500, "Description");

        ColumnModel<TraceInfo> model = new ColumnModel<TraceInfo>(Arrays.<ColumnConfig<TraceInfo, ?>>asList(
                clockCol, durationCol, callsCol, errorsCol, recordsCol, descCol));

        clockCol.setCell(new AbstractCell<Long>() {
            @Override
            public void render(Context context, Long clock, SafeHtmlBuilder sb) {
                sb.appendHtmlConstant("<span>");
                sb.append(SafeHtmlUtils.fromString(ClientUtil.formatTimestamp(clock)));
                sb.appendHtmlConstant("</span>");
            }
        });

        durationCol.setCell(new AbstractCell<Long>() {
            @Override
            public void render(Context context, Long time, SafeHtmlBuilder sb) {
                String strTime = ClientUtil.formatDuration(time);
                sb.appendHtmlConstant("<span>");
                sb.append(SafeHtmlUtils.fromString(strTime));
                sb.appendHtmlConstant("</span>");
            }
        });

        traceStore = new ListStore<TraceInfo>(new ModelKeyProvider<TraceInfo>() {
            @Override
            public String getKey(TraceInfo item) {
                return "" + item.getDataOffs();
            }
        });

        traceGridView = new LiveGridView<TraceInfo>();
        traceGridView.setAutoExpandColumn(descCol);
        traceGridView.setForceFit(true);

        traceProxy = new DataProxy<PagingLoadConfig, PagingLoadResult<TraceInfo>>() {
            @Override
            public void load(final PagingLoadConfig loadConfig, final Callback<PagingLoadResult<TraceInfo>, Throwable> callback) {
                if (selectedHost != null) {
                    tds.pageTraces(selectedHost.getId(), loadConfig.getOffset(), loadConfig.getLimit(),
                            new MethodCallback<PagingData<TraceInfo>>() {
                                @Override
                                public void onFailure(Method method, Throwable exception) {
                                    callback.onFailure(exception);
                                }

                                @Override
                                public void onSuccess(Method method, PagingData<TraceInfo> response) {
                                    PagingLoadResultBean<TraceInfo> result = new PagingLoadResultBean<TraceInfo>(
                                            response.getResults(), response.getTotal(), response.getOffset());
                                    callback.onSuccess(result);
                                }
                            });
                }
            }
        };

        traceLoader = new PagingLoader<PagingLoadConfig, PagingLoadResult<TraceInfo>>(traceProxy);
        traceLoader.setRemoteSort(false);

        traceGrid = new Grid<TraceInfo>(traceStore, model) {
            @Override
            protected void onAfterFirstAttach() {
                super.onAfterFirstAttach();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        traceLoader.load(0, traceGridView.getCacheSize());
                    }
                });
            }
        };

        traceGrid.setLoadMask(true);
        traceGrid.setLoader(traceLoader);
        traceGrid.setView(traceGridView);

        traceGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
        traceGrid.addCellDoubleClickHandler(new CellDoubleClickEvent.CellDoubleClickHandler() {
            @Override
            public void onCellClick(CellDoubleClickEvent event) {
                TraceInfo traceInfo = traceGrid.getSelectionModel().getSelectedItem();
                TraceDetailPanel detail = new TraceDetailPanel(tds, traceInfo);
                shell.addView(detail, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + selectedHost.getName());
            }
        });

        add(traceGrid, new VerticalLayoutData(1, 1));
    }

}
