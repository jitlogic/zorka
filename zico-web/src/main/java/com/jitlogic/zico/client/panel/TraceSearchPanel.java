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
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.data.SymbolProxy;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceInfoSearchQueryProxy;
import com.jitlogic.zico.shared.data.TraceInfoSearchResultProxy;
import com.jitlogic.zico.shared.services.TraceDataServiceProxy;
import com.sencha.gxt.data.shared.LabelProvider;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.button.ToggleButton;
import com.sencha.gxt.widget.core.client.container.BoxLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SimpleComboBox;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.form.validator.RegExValidator;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.tips.ToolTipConfig;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraceSearchPanel extends VerticalLayoutContainer {

    public final static String RE_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}\\s*(\\d{2}:\\d{2}:\\d{2}(\\.\\d{1-3})?)?";

    private PanelFactory pf;
    private ZicoRequestFactory rf;

    private Provider<ZicoShell> shell;

    private HostProxy host;

    private DataGrid<TraceInfoProxy> grid;
    private ListDataProvider<TraceInfoProxy> data;
    private SingleSelectionModel<TraceInfoProxy> selection;

    private Map<Integer, String> traceTypes;

    private ToggleButton btnErrors;
    private TextField txtClockEnd;
    private TextField txtClockBegin;
    private TextField txtFilter;
    private SpinnerField<Double> txtDuration;
    private SimpleComboBox<Integer> cmbTraceType;

    private TraceSearchTableBuilder rowBuilder;

    private Set<Long> expandedDetails = new HashSet<Long>();

    private ErrorHandler errorHandler;
    private Menu contextMenu;


    @Inject
    public TraceSearchPanel(Provider<ZicoShell> shell, ZicoRequestFactory rf,
                            PanelFactory pf, @Assisted HostProxy host,
                            ErrorHandler errorHandler) {
        this.shell = shell;
        this.rf = rf;
        this.pf = pf;
        this.host = host;
        this.errorHandler = errorHandler;

        traceTypes = new HashMap<Integer, String>();
        traceTypes.put(0, "(all)");

        createToolbar();
        createTraceGrid();
        createContextMenu();

        loadTraceTypes();
        refresh();
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
                refresh();
            }
        });

        btnErrors = new ToggleButton();
        btnErrors.setIcon(Resources.INSTANCE.errorMarkIcon());
        btnErrors.setToolTip("Show only erros.");

        toolBar.add(btnErrors);

        btnErrors.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                refresh();
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
                refresh();
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
                refresh();
            }
        });

        txtDuration.addSelectionHandler(new SelectionHandler<Double>() {
            @Override
            public void onSelection(SelectionEvent<Double> event) {
                refresh();
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
                    refresh();
                }
            }
        });

        txtClockBegin.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                if (txtClockBegin.isValid()) {
                    refresh();
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
                    refresh();
                }
            }
        });

        txtClockEnd.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                refresh();
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
                    refresh();
                }
            }
        });

        txtFilter.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                if (txtClockEnd.isValid()) {
                    refresh();
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

                refresh();
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
    }

    private void createTraceGrid() {
        grid = new DataGrid<TraceInfoProxy>(1024*1024, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceInfoProxy>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        // TODO detail expander cell here

        Column<TraceInfoProxy, TraceInfoProxy> colExpander
                = new IdentityColumn<TraceInfoProxy>(DETAIL_EXPANDER_CELL);
        grid.addColumn(colExpander, "#");
        grid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceClock
                = new IdentityColumn<TraceInfoProxy>(TRACE_CLOCK_CELL);
        grid.addColumn(colTraceClock, "Time");
        grid.setColumnWidth(colTraceClock, 100, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceType
                = new IdentityColumn<TraceInfoProxy>(TRACE_TYPE_CELL);
        grid.addColumn(colTraceType, "Type");
        grid.setColumnWidth(colTraceType, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceDuration
                = new IdentityColumn<TraceInfoProxy>(TRACE_DURATION_CELL);
        grid.addColumn(colTraceDuration, "Duration");
        grid.setColumnWidth(colTraceDuration, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceCalls
                = new IdentityColumn<TraceInfoProxy>(TRACE_CALLS_CELL);
        grid.addColumn(colTraceCalls, "Calls");
        grid.setColumnWidth(colTraceCalls, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceErrors
                = new IdentityColumn<TraceInfoProxy>(TRACE_ERRORS_CELL);
        grid.addColumn(colTraceErrors, "Errors");
        grid.setColumnWidth(colTraceErrors, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceRecords
                = new IdentityColumn<TraceInfoProxy>(TRACE_RECORDS_CELL);
        grid.addColumn(colTraceRecords, "Records");
        grid.setColumnWidth(colTraceRecords, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceDesc
                = new IdentityColumn<TraceInfoProxy>(TRACE_NAME_CELL);
        grid.addColumn(colTraceDesc, "Description");
        grid.setColumnWidth(colTraceDesc, 100, Style.Unit.PCT);

        rowBuilder = new TraceSearchTableBuilder(grid, expandedDetails);
        grid.setTableBuilder(rowBuilder);

        grid.setSkipRowHoverStyleUpdate(true);
        grid.setSkipRowHoverFloatElementCheck(true);
        grid.setSkipRowHoverCheck(true);
        grid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        data = new ListDataProvider<TraceInfoProxy>();
        data.addDataDisplay(grid);

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceInfoProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceInfoProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selection.setSelected(event.getValue(), true);
                    openDetailView();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selection.setSelected(event.getValue(), true);
                    contextMenu.showAt(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
                }
            }
        });

        grid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        grid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        add(grid, new VerticalLayoutData(1, 1));
    }


    private void createContextMenu() {
        contextMenu = new Menu();

        MenuItem mnuMethodTree = new MenuItem("Method call tree");
        mnuMethodTree.setIcon(Resources.INSTANCE.methodTreeIcon());
        mnuMethodTree.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openDetailView();
            }
        });
        contextMenu.add(mnuMethodTree);

        MenuItem mnuMethodRank = new MenuItem("Method call stats");
        mnuMethodRank.setIcon(Resources.INSTANCE.methodRankIcon());
        mnuMethodRank.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openRankingView();
            }
        });
        contextMenu.add(mnuMethodRank);

        contextMenu.add(new SeparatorMenuItem());

        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes");
        mnuMethodAttrs.setIcon(Resources.INSTANCE.methodAttrsIcon());
        mnuMethodAttrs.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openMethodAttrsDialog();
            }
        });
        contextMenu.add(mnuMethodAttrs);
    }


    private void openDetailView() {
        TraceInfoProxy traceInfo = selection.getSelectedObject();
        if (traceInfo != null) {
            TraceCallTreePanel detail = pf.traceCallTreePanel(traceInfo);
            shell.get().addView(detail, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + host.getName());
        }
    }


    private void openRankingView() {
        TraceInfoProxy traceInfo = selection.getSelectedObject();
        if (traceInfo != null) {
            MethodRankingPanel ranking = pf.methodRankingPanel(traceInfo);
            shell.get().addView(ranking, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + host.getName());
        }
    }

    private void openMethodAttrsDialog() {
        TraceInfoProxy ti = selection.getSelectedObject();
        if (ti != null) {
            pf.methodAttrsDialog(ti.getHostName(), ti.getDataOffs(), "", 0L).show();
        }
    }

    private void refresh() {
        TraceDataServiceProxy req = rf.traceDataService();
        TraceInfoSearchQueryProxy q = req.create(TraceInfoSearchQueryProxy.class);
        q.setLimit(50);
        q.setHostName(host.getName());

        req.searchTraces(q).fire(new Receiver<TraceInfoSearchResultProxy>() {
            @Override
            public void onSuccess(TraceInfoSearchResultProxy response) {
                List<TraceInfoProxy> results = response.getResults();
                data.getList().addAll(results);
            }
        });
    }

    private void loadTraceTypes() {
        rf.systemService().getTidMap(host.getName()).fire(new Receiver<List<SymbolProxy>>() {
            @Override
            public void onSuccess(List<SymbolProxy> response) {
                for (SymbolProxy e : response) {
                    traceTypes.put(e.getId(), e.getName());
                    cmbTraceType.add(e.getId());
                }
            }
        });
    }

    private void toggleDetails(TraceInfoProxy ti) {
        long offs = ti.getDataOffs();
        if (expandedDetails.contains(offs)) {
            expandedDetails.remove(offs);
        } else {
            expandedDetails.add(offs);
        }
        grid.redrawRow(data.getList().indexOf(ti));
    }

    private static final ProvidesKey<TraceInfoProxy> KEY_PROVIDER = new ProvidesKey<TraceInfoProxy>() {
        @Override
        public Object getKey(TraceInfoProxy item) {
            return item.getDataOffs();
        }
    };

    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();

    private static final String EXPANDER_EXPAND = AbstractImagePrototype.create(Resources.INSTANCE.expanderExpand()).getHTML();
    private static final String EXPANDER_COLLAPSE = AbstractImagePrototype.create(Resources.INSTANCE.expanderCollapse()).getHTML();

    private final Cell<TraceInfoProxy> DETAIL_EXPANDER_CELL = new ActionCell<TraceInfoProxy>("",
            new ActionCell.Delegate<TraceInfoProxy>() {
                @Override
                public void execute(TraceInfoProxy rec) {
                    toggleDetails(rec);
                }
            }) {
        @Override
        public void render(Cell.Context context, TraceInfoProxy tr, SafeHtmlBuilder sb) {
            if ((tr.getAttributes() != null && tr.getAttributes().size() > 0)||tr.getExceptionInfo() != null) {
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(expandedDetails.contains(tr.getDataOffs()) ? EXPANDER_COLLAPSE : EXPANDER_EXPAND);
                sb.appendHtmlConstant("</span>");
            }
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_NAME_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy ti, SafeHtmlBuilder sb) {
            String color = ti.getStatus() != 0 ? "red" : "black";
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\" style=\"color: " + color + "; text-align: left;\">");
            sb.append(SafeHtmlUtils.fromString(ti.getDescription()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_CLOCK_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatTimestamp(rec.getClock())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_TYPE_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getTraceType()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_DURATION_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getExecutionTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_CALLS_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getCalls()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_RECORDS_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getRecords()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_ERRORS_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getErrors()));
            sb.appendHtmlConstant("</div>");
        }
    };


}
