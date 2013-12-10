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
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.*;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.props.TraceInfoProperties;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.data.KeyValueProxy;
import com.jitlogic.zico.shared.data.PagingDataProxy;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceListFilterProxy;
import com.jitlogic.zico.shared.services.TraceDataServiceProxy;
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
import com.sencha.gxt.widget.core.client.tips.ToolTipConfig;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceListPanel extends VerticalLayoutContainer {

    private static final TraceInfoProperties props = GWT.create(TraceInfoProperties.class);

    public final static String RE_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}\\s*(\\d{2}:\\d{2}:\\d{2}(\\.\\d{1-3})?)?";

    private PanelFactory panelFactory;
    private ZicoRequestFactory rf;

    private HostProxy selectedHost;
    private Grid<TraceInfoProxy> traceGrid;
    private ListStore<TraceInfoProxy> traceStore;
    private DataProxy<PagingLoadConfig, PagingLoadResult<TraceInfoProxy>> traceProxy;
    private PagingLoader<PagingLoadConfig, PagingLoadResult<TraceInfoProxy>> traceLoader;
    private LiveGridView<TraceInfoProxy> traceGridView;

    private Provider<ZicoShell> shell;
    private ToggleButton btnErrors;
    private TextField txtClockEnd;
    private TextField txtClockBegin;
    private TextField txtFilter;
    private SpinnerField<Double> txtDuration;
    private SimpleComboBox<Integer> cmbTraceType;

    private Map<Integer, String> traceTypes;

    private ErrorHandler errorHandler;


    @Inject
    public TraceListPanel(Provider<ZicoShell> shell, ZicoRequestFactory rf,
                          PanelFactory panelFactory, @Assisted HostProxy hostInfo,
                          ErrorHandler errorHandler) {
        this.shell = shell;
        this.rf = rf;
        this.selectedHost = hostInfo;
        this.panelFactory = panelFactory;
        this.errorHandler = errorHandler;

        traceTypes = new HashMap<Integer, String>();
        traceTypes.put(0, "(all)");

        createToolbar();
        createTraceListGrid();
        createContextMenu();
        loadTraceTypes();
    }


    private void createTraceListGrid() {

        ColumnConfig<TraceInfoProxy, Long> clockCol = new ColumnConfig<TraceInfoProxy, Long>(props.clock(), 100, "Time");
        clockCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfoProxy, Long> durationCol = new ColumnConfig<TraceInfoProxy, Long>(props.executionTime(), 50, "Duration");
        durationCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfoProxy, Long> callsCol = new ColumnConfig<TraceInfoProxy, Long>(props.calls(), 50, "Calls");
        callsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfoProxy, Long> errorsCol = new ColumnConfig<TraceInfoProxy, Long>(props.errors(), 50, "Errors");
        errorsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfoProxy, Long> recordsCol = new ColumnConfig<TraceInfoProxy, Long>(props.records(), 50, "Records");
        recordsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<TraceInfoProxy, String> traceTypeCol = new ColumnConfig<TraceInfoProxy, String>(props.traceType(), 50, "Type");
        traceTypeCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        traceTypeCol.setSortable(false);
        traceTypeCol.setMenuDisabled(true);

        ColumnConfig<TraceInfoProxy, TraceInfoProxy> descCol = new ColumnConfig<TraceInfoProxy, TraceInfoProxy>(
                new IdentityValueProvider<TraceInfoProxy>(), 500, "Description");

        descCol.setSortable(false);
        descCol.setMenuDisabled(true);

        TraceDetailCell traceDetailCell = new TraceDetailCell();

        RowExpander<TraceInfoProxy> expander = new RowExpander<TraceInfoProxy>(
                new IdentityValueProvider<TraceInfoProxy>(), traceDetailCell);

        ColumnModel<TraceInfoProxy> model = new ColumnModel<TraceInfoProxy>(Arrays.<ColumnConfig<TraceInfoProxy, ?>>asList(
                expander, clockCol, traceTypeCol, durationCol, callsCol, errorsCol, recordsCol, descCol));

        clockCol.setCell(new AbstractCell<Long>() {
            @Override
            public void render(Context context, Long clock, SafeHtmlBuilder sb) {
                sb.appendHtmlConstant("<span>");
                sb.append(SafeHtmlUtils.fromString(ClientUtil.formatTimestamp(clock)));
                sb.appendHtmlConstant("</span>");
            }
        });

        durationCol.setCell(new NanoTimeRenderingCell());

        descCol.setCell(new AbstractCell<TraceInfoProxy>() {
            @Override
            public void render(Context context, TraceInfoProxy ti, SafeHtmlBuilder sb) {
                String color = ti.getStatus() != 0 ? "red" : "black";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(ti.getDescription()));
                sb.appendHtmlConstant("</span>");
            }
        });

        traceStore = new ListStore<TraceInfoProxy>(new ModelKeyProvider<TraceInfoProxy>() {
            @Override
            public String getKey(TraceInfoProxy item) {
                return "" + item.getDataOffs();
            }
        });

        traceGridView = new LiveGridView<TraceInfoProxy>();
        traceGridView.setAutoExpandColumn(descCol);
        traceGridView.setForceFit(true);

        traceProxy = new DataProxy<PagingLoadConfig, PagingLoadResult<TraceInfoProxy>>() {
            @Override
            public void load(final PagingLoadConfig loadConfig, final Callback<PagingLoadResult<TraceInfoProxy>, Throwable> callback) {
                filterAndLoadData(loadConfig, callback);
            }
        };

        traceLoader = new PagingLoader<PagingLoadConfig, PagingLoadResult<TraceInfoProxy>>(traceProxy);
        traceLoader.setRemoteSort(false);

        traceGrid = new Grid<TraceInfoProxy>(traceStore, model) {
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
        TraceInfoProxy traceInfo = traceGrid.getSelectionModel().getSelectedItem();
        if (traceInfo != null) {
            TraceDetailPanel detail = panelFactory.traceDetailPanel(traceInfo);
            shell.get().addView(detail, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + selectedHost.getName());
        }
    }

    private void openRankingView() {
        TraceInfoProxy traceInfo = traceGrid.getSelectionModel().getSelectedItem();
        if (traceInfo != null) {
            MethodRankingPanel ranking = panelFactory.methodRankingPanel(traceInfo);
            shell.get().addView(ranking, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + selectedHost.getName());
        }
    }


    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        TextButton btnRefresh = new TextButton();
        btnRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        btnRefresh.setToolTip("Refresh list.");
        toolBar.add(btnRefresh);

        btnRefresh.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                traceGridView.refresh();
            }
        });

        btnErrors = new ToggleButton();
        btnErrors.setIcon(Resources.INSTANCE.errorMarkIcon());
        btnErrors.setToolTip("Show only erros.");

        toolBar.add(btnErrors);

        btnErrors.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                traceGridView.refresh();
            }
        });

        cmbTraceType = new SimpleComboBox<Integer>(new LabelProvider<Integer>() {
            @Override
            public String getLabel(Integer item) {
                return traceTypes.get(item);
            }
        });

        cmbTraceType.setForceSelection(true);
        cmbTraceType.setToolTip("Select trace type here.");
        cmbTraceType.add(0);

        cmbTraceType.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                traceGridView.refresh();
            }
        });

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

        txtDuration.addValueChangeHandler(new ValueChangeHandler<Double>() {
            @Override
            public void onValueChange(ValueChangeEvent<Double> event) {
                traceGridView.refresh();
            }
        });

        txtDuration.addSelectionHandler(new SelectionHandler<Double>() {
            @Override
            public void onSelection(SelectionEvent<Double> event) {
                traceGridView.refresh();
            }
        });

        txtFilter = new TextField();
        BoxLayoutContainer.BoxLayoutData txtFilterLayout = new BoxLayoutContainer.BoxLayoutData();
        txtFilterLayout.setFlex(1.0);

        ToolTipConfig ttcFilter = new ToolTipConfig("Text search:" +
                "<li><b>sometext</b> - full-text search</li>"
                + "<li><b>~regex</b> - regular expression search</li>"
                + "<li><b>@ATTR=sometext</b> - full text search in specific attribute</li>"
                + "<li><b>@ATTR~=regex</b> - regex search in specific attribute</li>");

        txtFilter.setToolTipConfig(ttcFilter);

        txtFilter.setLayoutData(txtFilterLayout);
        toolBar.add(txtFilter);

        Label lblBetween = new Label("between :");
        toolBar.add(lblBetween);

        ToolTipConfig ttcDateTime = new ToolTipConfig("Allowed timestamp formats:" +
                "<li><b>YYYY-MM-DD</b> - date only</li>" +
                "<li><b>YYYY-MM-DD hh:mm:ss</b> - date and time</li>" +
                "<li><b>YYYY-MM-DD hh:mm:ss.SSS</b> - millisecond resolution</li>");

        txtClockBegin = new TextField();
        txtClockBegin.setWidth(130);
        txtClockBegin.setToolTipConfig(ttcDateTime);
        txtClockBegin.addValidator(new RegExValidator(RE_TIMESTAMP, "Enter valid timestamp."));
        txtClockBegin.setEmptyText("Start time");

        toolBar.add(txtClockBegin);

        txtClockBegin.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && txtClockBegin.isValid()) {
                    traceGridView.refresh();
                }
            }
        });

        txtClockBegin.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                if (txtClockBegin.isValid()) {
                    traceGridView.refresh();
                }
            }
        });

        toolBar.add(new Label("and :"));

        txtClockEnd = new TextField();
        txtClockEnd.setWidth(130);
        txtClockEnd.setToolTipConfig(ttcDateTime);
        txtClockEnd.addValidator(new RegExValidator(RE_TIMESTAMP, "Enter valid timestamp."));
        txtClockEnd.setEmptyText("End time");

        toolBar.add(txtClockEnd);

        txtClockEnd.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    traceGridView.refresh();
                }
            }
        });

        txtClockEnd.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                traceGridView.refresh();
            }
        });

        TextButton btnClear = new TextButton();
        btnClear.setIcon(Resources.INSTANCE.clearIcon());
        btnClear.setToolTip("Clear all filters.");
        toolBar.add(btnClear);

        txtFilter.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && txtClockEnd.isValid()) {
                    traceGridView.refresh();
                }
            }
        });

        txtFilter.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                if (txtClockEnd.isValid()) {
                    traceGridView.refresh();
                }
            }
        });

        btnClear.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                txtFilter.setText("");
                txtDuration.setText("");
                btnErrors.setValue(false);
                cmbTraceType.setValue(null);

                traceGridView.refresh();
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
    }


    private void filterAndLoadData(PagingLoadConfig loadConfig, final Callback<PagingLoadResult<TraceInfoProxy>, Throwable> callback) {
        if (selectedHost != null) {
            List<? extends SortInfo> sort = loadConfig.getSortInfo();
            TraceDataServiceProxy req = rf.traceDataService();
            TraceListFilterProxy filter = req.create(TraceListFilterProxy.class);

            filter.setFilterExpr(txtFilter.getText());
            if (cmbTraceType.getCurrentValue() != null) {
                filter.setTraceId(cmbTraceType.getCurrentValue());
            }
            if (txtDuration.getCurrentValue() != null) {
                filter.setMinTime((long) (txtDuration.getCurrentValue() * 1000000000L));
            } else {
                filter.setMinTime(0);
            }
            if (txtClockBegin.getValue() != null) {
                filter.setTimeStart(ClientUtil.parseTimestamp(txtClockBegin.getValue(), "00:00:00.000"));
            }
            if (txtClockEnd.getValue() != null) {
                filter.setTimeEnd(ClientUtil.parseTimestamp(txtClockEnd.getValue(), "23:59:59.999"));
            }

            filter.setErrorsOnly(btnErrors.getValue());
            filter.setSortBy(sort.size() > 0 ? sort.get(0).getSortField() : "clock");
            filter.setSortAsc(sort.size() > 0 ? sort.get(0).getSortDir().name().equals("ASC") : false);

            req.pageTraces(selectedHost.getId(), loadConfig.getOffset(), loadConfig.getLimit(),
                    filter).fire(new Receiver<PagingDataProxy>() {
                @Override
                public void onSuccess(PagingDataProxy response) {
                    PagingLoadResultBean<TraceInfoProxy> result = new PagingLoadResultBean<TraceInfoProxy>(
                            response.getResults(), response.getTotal(), response.getOffset());
                    callback.onSuccess(result);
                }
            });
        }
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

        MenuItem mnuMethodRank = new MenuItem("Method call stats");
        mnuMethodRank.setIcon(Resources.INSTANCE.methodRankIcon());
        menu.add(mnuMethodRank);

        mnuMethodRank.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openRankingView();
            }
        });

        menu.add(new SeparatorMenuItem());

        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes");
        mnuMethodAttrs.setIcon(Resources.INSTANCE.methodAttrsIcon());
        menu.add(mnuMethodAttrs);

        mnuMethodAttrs.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                TraceInfoProxy ti = traceGrid.getSelectionModel().getSelectedItem();
                MethodAttrsDialog dialog = panelFactory.methodAttrsDialog(ti.getHostId(), ti.getDataOffs(), "", 0L);
                dialog.show();
            }
        });


        traceGrid.setContextMenu(menu);
    }

    private void loadTraceTypes() {
        rf.systemService().getTidMap(selectedHost.getId()).fire(new Receiver<List<KeyValueProxy>>() {
            @Override
            public void onSuccess(List<KeyValueProxy> response) {
                for (KeyValueProxy e : response) {
                    int tid = Integer.parseInt(e.getKey());
                    traceTypes.put(tid, e.getValue());
                    cmbTraceType.add(tid);
                }
            }
        });
    }

}
