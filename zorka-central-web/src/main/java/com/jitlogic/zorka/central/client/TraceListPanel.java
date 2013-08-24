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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.jitlogic.zorka.central.data.*;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.SortInfo;
import com.sencha.gxt.data.shared.loader.*;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.button.ToggleButton;
import com.sencha.gxt.widget.core.client.container.BoxLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.*;
import com.sencha.gxt.widget.core.client.grid.*;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Arrays;
import java.util.List;

public class TraceListPanel extends VerticalLayoutContainer {

    private static final TraceInfoProperties props = GWT.create(TraceInfoProperties.class);

    private TraceDataService tds;

    private HostInfo selectedHost;
    private Grid<TraceInfo> traceGrid;
    private ListStore<TraceInfo> traceStore;
    private DataProxy<PagingLoadConfig, PagingLoadResult<TraceInfo>> traceProxy;
    private PagingLoader<PagingLoadConfig, PagingLoadResult<TraceInfo>> traceLoader;
    private LiveGridView<TraceInfo> traceGridView;

    TraceListFilterExpression filter = new TraceListFilterExpression();

    private ZorkaCentralShell shell;

    public TraceListPanel(ZorkaCentralShell shell, TraceDataService tds, HostInfo hostInfo) {
        this.shell = shell;
        this.tds = tds;
        this.selectedHost = hostInfo;

        filter.setSortBy("clock");
        filter.setSortAsc(false);

        createToolbar();
        createTraceListGrid();
    }


    private void createTraceListGrid() {
        ColumnConfig<TraceInfo, Long> clockCol = new ColumnConfig<TraceInfo, Long>(props.clock(), 100, "Clock");
        clockCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> durationCol = new ColumnConfig<TraceInfo, Long>(props.executionTime(), 50, "Time");
        durationCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> callsCol = new ColumnConfig<TraceInfo, Long>(props.calls(), 50, "Calls");
        callsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> errorsCol = new ColumnConfig<TraceInfo, Long>(props.errors(), 50, "Errors");
        errorsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> recordsCol = new ColumnConfig<TraceInfo, Long>(props.records(), 50, "Records");
        recordsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

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
                    List<? extends SortInfo> sort = loadConfig.getSortInfo();
                    filter.setSortBy(sort.size() > 0 ? sort.get(0).getSortField() : "clock");
                    filter.setSortAsc(sort.size() > 0 ? sort.get(0).getSortDir().name().equals("ASC") : false);
                    tds.pageTraces(selectedHost.getId(), loadConfig.getOffset(), loadConfig.getLimit(),
                            filter,
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

        //setCenterWidget(traceGrid);
        add(traceGrid, new VerticalLayoutData(1, 1));
    }


    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        TextButton btnRefresh = new TextButton();
        btnRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        btnRefresh.setToolTip("Refresh data");
        toolBar.add(btnRefresh);

        btnRefresh.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                traceGridView.refresh();
            }
        });

        toolBar.add(new SeparatorToolItem());

        ToggleButton btnErrors = new ToggleButton();
        btnErrors.setIcon(Resources.INSTANCE.errorMarkIcon());
        btnErrors.setToolTip("Show only error traces.");

        toolBar.add(btnErrors);

        toolBar.add(new SeparatorToolItem());

//        TextButton btnSetStart = new TextButton();
//        btnSetStart.setIcon(Resources.INSTANCE.goNextIcon());
//        btnSetStart.setToolTip("Go to specific point in time.");
//        toolBar.add(btnSetStart);
//
//        DateField txtStartDate = new DateField();
//        txtStartDate.setWidth(100);
//        toolBar.add(txtStartDate);
//
//        TimeField txtStartTime = new TimeField();
//        txtStartTime.setWidth(80);
//        toolBar.add(txtStartTime);
//
//        toolBar.add(new SeparatorToolItem());

        TextButton btnFilter = new TextButton();
        btnFilter.setIcon(Resources.INSTANCE.filterIcon());
        btnFilter.setToolTip("Filter by criteria");
        toolBar.add(btnFilter);

        final SpinnerField<Double> txtDuration = new SpinnerField<Double>(new NumberPropertyEditor.DoublePropertyEditor());
        txtDuration.setIncrement(1d);
        txtDuration.setMinValue(0);
        txtDuration.setMaxValue(1000000d);
        txtDuration.setAllowNegative(false);
        txtDuration.setAllowBlank(true);
        txtDuration.setWidth(80);
        txtDuration.setToolTip("Minimum trace execution time (in seconds)");
        toolBar.add(txtDuration);

        final TextField txtFilter = new TextField();
        BoxLayoutContainer.BoxLayoutData txtFilterLayout = new BoxLayoutContainer.BoxLayoutData();
        txtFilterLayout.setFlex(1.0);
        txtFilter.setToolTip("Search for text (as in Description field)");
        txtFilter.setLayoutData(txtFilterLayout);
        toolBar.add(txtFilter);

        btnFilter.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                GWT.log("Setting filter to " + txtFilter.getText());
                filter.setFilterExpr(txtFilter.getValue());
                if (txtDuration.getCurrentValue() != null) {
                    filter.setMinTime((long) (txtDuration.getCurrentValue() * 1000000000L));
                } else {
                    filter.setMinTime(0);
                }
                traceGridView.refresh();
            }
        });


        add(toolBar, new VerticalLayoutData(1, -1));
    }

}
