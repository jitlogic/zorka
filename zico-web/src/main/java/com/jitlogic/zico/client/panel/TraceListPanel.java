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
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.data.*;
import com.jitlogic.zico.client.api.AdminApi;
import com.jitlogic.zico.client.api.TraceDataApi;
import com.sencha.gxt.core.client.IdentityValueProvider;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.LabelProvider;
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
import com.sencha.gxt.widget.core.client.form.validator.RegExValidator;
import com.sencha.gxt.widget.core.client.grid.*;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceListPanel extends VerticalLayoutContainer {

    private static final TraceInfoProperties props = GWT.create(TraceInfoProperties.class);

    private TraceDataApi tds;
    private AdminApi ads;
    private PanelFactory panelFactory;

    private HostInfo selectedHost;
    private Grid<TraceInfo> traceGrid;
    private ListStore<TraceInfo> traceStore;
    private DataProxy<PagingLoadConfig, PagingLoadResult<TraceInfo>> traceProxy;
    private PagingLoader<PagingLoadConfig, PagingLoadResult<TraceInfo>> traceLoader;
    private LiveGridView<TraceInfo> traceGridView;

    private TraceListFilterExpression filter = new TraceListFilterExpression();

    private Provider<ZicoShell> shell;
    private ToggleButton btnErrors;
    private TextField txtClockEnd;
    private TextField txtClockBegin;
    private TextField txtFilter;
    private SpinnerField<Double> txtDuration;
    private SimpleComboBox<Integer> cmbTraceType;

    private Map<Integer, String> traceTypes;

    @Inject
    public TraceListPanel(Provider<ZicoShell> shell, TraceDataApi tds, AdminApi ads,
                          PanelFactory panelFactory, @Assisted HostInfo hostInfo) {
        this.shell = shell;
        this.tds = tds;
        this.ads = ads;
        this.selectedHost = hostInfo;
        this.panelFactory = panelFactory;

        filter.setSortBy("clock");
        filter.setSortAsc(false);

        traceTypes = new HashMap<Integer, String>();
        traceTypes.put(0, "(all)");

        createToolbar();
        createTraceListGrid();
        createContextMenu();
        loadTraceTypes();
    }


    private void createTraceListGrid() {

        ColumnConfig<TraceInfo, Long> clockCol = new ColumnConfig<TraceInfo, Long>(props.clock(), 100, "Time");
        clockCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> durationCol = new ColumnConfig<TraceInfo, Long>(props.executionTime(), 50, "Duration");
        durationCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> callsCol = new ColumnConfig<TraceInfo, Long>(props.calls(), 50, "Calls");
        callsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> errorsCol = new ColumnConfig<TraceInfo, Long>(props.errors(), 50, "Errors");
        errorsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, Long> recordsCol = new ColumnConfig<TraceInfo, Long>(props.records(), 50, "Records");
        recordsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, String> traceTypeCol = new ColumnConfig<TraceInfo, String>(props.traceType(), 50, "Type");
        traceTypeCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfo, TraceInfo> descCol = new ColumnConfig<TraceInfo, TraceInfo>(
                new IdentityValueProvider<TraceInfo>(), 500, "Description");

        descCol.setSortable(false);
        descCol.setMenuDisabled(true);

        TraceDetailCell traceDetailCell = new TraceDetailCell();

        RowExpander<TraceInfo> expander = new RowExpander<TraceInfo>(
                new IdentityValueProvider<TraceInfo>(), traceDetailCell);

        ColumnModel<TraceInfo> model = new ColumnModel<TraceInfo>(Arrays.<ColumnConfig<TraceInfo, ?>>asList(
                expander, clockCol, traceTypeCol, durationCol, callsCol, errorsCol, recordsCol, descCol));

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

        descCol.setCell(new AbstractCell<TraceInfo>() {
            @Override
            public void render(Context context, TraceInfo ti, SafeHtmlBuilder sb) {
                String color = ti.getStatus() != 0 ? "red" : "black";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(ti.getDescription()));
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
                openDetailView();
            }
        });

        expander.initPlugin(traceGrid);

        add(traceGrid, new VerticalLayoutData(1, 1));
    }

    private void openDetailView() {
        TraceInfo traceInfo = traceGrid.getSelectionModel().getSelectedItem();
        TraceDetailPanel detail = panelFactory.traceDetailPanel(traceInfo);
        shell.get().addView(detail, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + selectedHost.getName());
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

        btnErrors = new ToggleButton();
        btnErrors.setIcon(Resources.INSTANCE.errorMarkIcon());
        btnErrors.setToolTip("Show only error traces.");

        toolBar.add(btnErrors);

        btnErrors.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                filter.setErrorsOnly(event.getValue());
                traceGridView.refresh();
            }
        });

        toolBar.add(new SeparatorToolItem());

        TextButton btnFilter = new TextButton();
        btnFilter.setIcon(Resources.INSTANCE.filterIcon());
        btnFilter.setToolTip("Filter by criteria");
        toolBar.add(btnFilter);


        cmbTraceType = new SimpleComboBox<Integer>(new LabelProvider<Integer>() {
            @Override
            public String getLabel(Integer item) {
                return traceTypes.get(item);
            }
        });

        cmbTraceType.setForceSelection(true);
        cmbTraceType.setToolTip("Trace type.");
        cmbTraceType.add(0);

        toolBar.add(cmbTraceType);

        txtDuration = new SpinnerField<Double>(new NumberPropertyEditor.DoublePropertyEditor());
        txtDuration.setIncrement(1d);
        txtDuration.setMinValue(0);
        txtDuration.setMaxValue(1000000d);
        txtDuration.setAllowNegative(false);
        txtDuration.setAllowBlank(true);
        txtDuration.setWidth(80);
        txtDuration.setToolTip("Minimum trace execution time (in seconds)");
        toolBar.add(txtDuration);

        txtFilter = new TextField();
        BoxLayoutContainer.BoxLayoutData txtFilterLayout = new BoxLayoutContainer.BoxLayoutData();
        txtFilterLayout.setFlex(1.0);
        txtFilter.setToolTip("Search for text (as in Description field)");
        txtFilter.setLayoutData(txtFilterLayout);
        toolBar.add(txtFilter);

        txtClockBegin = new TextField();
        txtClockBegin.setWidth(130);
        //txtClockBegin.setToolTip("From trace timestamp.");
        txtClockBegin.addValidator(
                new RegExValidator("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "Enter YYYY-MM-DD hh:mm:ss timestamp."));
        txtClockBegin.setEmptyText("Start time");
        toolBar.add(txtClockBegin);

        txtClockEnd = new TextField();
        txtClockEnd.setWidth(130);
        //txtClockEnd.setWidth("To trace timestamp.");
        txtClockEnd.addValidator(
                new RegExValidator("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "Enter YYYY-MM-DD hh:mm:ss timestamp."));
        txtClockEnd.setEmptyText("End time");
        toolBar.add(txtClockEnd);

        TextButton btnClear = new TextButton();
        btnClear.setIcon(Resources.INSTANCE.clearIcon());
        btnClear.setToolTip("Clear all filters.");
        toolBar.add(btnClear);


        btnFilter.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                GWT.log("Setting filter to " + txtFilter.getText());
                filter.setFilterExpr(txtFilter.getText());
                filter.setTraceId(cmbTraceType.getCurrentValue());
                if (txtDuration.getCurrentValue() != null) {
                    filter.setMinTime((long) (txtDuration.getCurrentValue() * 1000000000L));
                } else {
                    filter.setMinTime(0);
                }
                filter.setTimeStart(ClientUtil.parseTimestamp(txtClockBegin.getValue()));
                filter.setTimeEnd(ClientUtil.parseTimestamp(txtClockEnd.getValue()));
                traceGridView.refresh();
            }
        });


        btnClear.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                txtFilter.setText("");
                txtDuration.setText("");
                btnErrors.setValue(false);

                filter.setErrorsOnly(false);
                filter.setMinTime(0);
                filter.setFilterExpr("");
                filter.setTimeStart(0);
                filter.setTimeEnd(0);
                filter.setTraceId(0);

                traceGridView.refresh();
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
    }

    private void createContextMenu() {
        Menu menu = new Menu();

        MenuItem mnuMethodTree = new MenuItem("Method call tree");
        mnuMethodTree.setIcon(Resources.INSTANCE.methodTreeIcon());
        menu.add(mnuMethodTree);

        mnuMethodTree.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openDetailView();
            }
        });

        menu.add(new SeparatorMenuItem());

        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes");
        mnuMethodAttrs.setIcon(Resources.INSTANCE.methodAttrsIcon());
        menu.add(mnuMethodAttrs);

        mnuMethodAttrs.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                MethodAttrsDialog dialog = new MethodAttrsDialog(traceGrid.getSelectionModel().getSelectedItem());
                dialog.show();
            }
        });

        traceGrid.setContextMenu(menu);
    }

    private void loadTraceTypes() {
        ads.getTidMap(new MethodCallback<Map<String, String>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling " + method, exception);
            }

            @Override
            public void onSuccess(Method method, Map<String, String> response) {
                for (Map.Entry<String, String> e : response.entrySet()) {
                    int tid = Integer.parseInt(e.getKey());
                    traceTypes.put(tid, e.getValue());
                    cmbTraceType.add(tid);
                }
            }
        });
    }

}
