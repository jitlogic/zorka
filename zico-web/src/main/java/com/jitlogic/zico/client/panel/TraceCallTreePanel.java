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
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TraceCallTreePanel extends VerticalLayoutContainer {

    private static final ProvidesKey<TraceRecordProxy> KEY_PROVIDER = new ProvidesKey<TraceRecordProxy>() {
        @Override
        public Object getKey(TraceRecordProxy item) {
            return item.getPath();
        }
    };

    private ZicoRequestFactory rf;
    private TraceInfoProxy trace;

    private TraceRecordSearchDialog searchDialog;
    private PanelFactory panelFactory;

    private DataGrid<TraceRecordProxy> grid;
    private SingleSelectionModel<TraceRecordProxy> selection;
    private ListDataProvider<TraceRecordProxy> data;
    private TraceCallTableBuilder rowBuilder;

    private Set<String> expandedDetails = new HashSet<String>();

    private SpinnerField<Double> txtDuration;

    private TextButton btnSearchPrev;
    private TextButton btnSearchNext;

    private boolean fullyExpanded;

    private List<TraceRecordProxy> searchResults = new ArrayList<TraceRecordProxy>();
    private int curentSearchResultIdx = -1;
    private TextButton btnExpandAll;

    @Inject
    public TraceCallTreePanel(ZicoRequestFactory rf, PanelFactory panelFactory, @Assisted TraceInfoProxy trace) {
        this.rf = rf;
        this.panelFactory = panelFactory;
        this.trace = trace;

        createToolbar();
        createCallTreeGrid();
        loadData(false, null);
    }


    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        TextButton btnParentMethod = new TextButton();
        btnParentMethod.setIcon(Resources.INSTANCE.goUpIcon());
        btnParentMethod.setToolTip("Go back to parent method");
        btnParentMethod.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                findParent();
            }
        });
        toolBar.add(btnParentMethod);

        TextButton btnSlowestMethod = new TextButton();
        btnSlowestMethod.setIcon(Resources.INSTANCE.goDownIcon());
        btnSlowestMethod.setToolTip("Drill down: slowest method");
        btnSlowestMethod.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                findSlowestMethod();
            }
        });
        toolBar.add(btnSlowestMethod);

        TextButton btnErrorMethod = new TextButton();
        btnErrorMethod.setIcon(Resources.INSTANCE.ligtningGo());
        btnErrorMethod.setToolTip("Go to next error");
        btnErrorMethod.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                findErrorMethod();
            }
        });
        toolBar.add(btnErrorMethod);

        toolBar.add(new SeparatorToolItem());

//        TextButton btnFilter = new TextButton();
//        btnFilter.setIcon(Resources.INSTANCE.clockIcon());
//        btnFilter.setToolTip("Filter by execution time.");
//        toolBar.add(btnFilter);
//
//        txtDuration = new SpinnerField<Double>(new NumberPropertyEditor.DoublePropertyEditor());
//        txtDuration.setIncrement(1d);
//        txtDuration.setMinValue(0);
//        txtDuration.setMaxValue(1000000d);
//        txtDuration.setAllowNegative(false);
//        txtDuration.setAllowBlank(true);
//        txtDuration.setWidth(100);
//        txtDuration.setToolTip("Minimum trace execution time (milliseconds)");
//        toolBar.add(txtDuration);
//
//        toolBar.add(new SeparatorToolItem());

        btnExpandAll = new TextButton();
        btnExpandAll.setIcon(Resources.INSTANCE.expandIcon());
        btnExpandAll.setToolTip("Expand all");
        btnExpandAll.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                loadData(true, null);
            }
        });
        toolBar.add(btnExpandAll);


        toolBar.add(new SeparatorToolItem());

        TextButton btnSearch = new TextButton();
        btnSearch.setIcon(Resources.INSTANCE.searchIcon());
        btnSearch.setToolTip("Search");
        btnSearch.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                doSearch();
            }
        });
        toolBar.add(btnSearch);

        btnSearchPrev = new TextButton();
        btnSearchPrev.setIcon(Resources.INSTANCE.goPrevIcon());
        btnSearchPrev.setToolTip("Go to previous search result");
        btnSearchPrev.setEnabled(false);
        btnSearchPrev.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                goToResult(curentSearchResultIdx-1);
            }
        });
        toolBar.add(btnSearchPrev);

        btnSearchNext = new TextButton();
        btnSearchNext.setIcon(Resources.INSTANCE.goNextIcon());
        btnSearchNext.setToolTip("Go to next search result");
        btnSearchNext.setEnabled(false);
        btnSearchNext.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                goToResult(curentSearchResultIdx+1);
            }
        });
        toolBar.add(btnSearchNext);


        add(toolBar, new VerticalLayoutData(1, -1));
    }


    private void createCallTreeGrid() {
        grid = new DataGrid<TraceRecordProxy>(1024*1024, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceRecordProxy>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        List<HasCell<TraceRecordProxy,?>> subCells = new ArrayList<HasCell<TraceRecordProxy, ?>>();

        subCells.add(new HasIdentityCell<TraceRecordProxy>(TREE_SPACER_CELL));
        subCells.add(new HasIdentityCell<TraceRecordProxy>(SUBTREE_EXPAND_CELL));
        subCells.add(new HasIdentityCell<TraceRecordProxy>(METHOD_NAME_CELL));
        //subCells.add(new HasIdentityCell<TraceRecordProxy>(DETAIL_EXPAND_CELL));

        Column<TraceRecordProxy, TraceRecordProxy> colExpander
                = new IdentityColumn<TraceRecordProxy>(DETAIL_EXPANDER_CELL);
        grid.addColumn(colExpander, " ");
        grid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceRecordProxy, TraceRecordProxy> colMethodName
                = new IdentityColumn<TraceRecordProxy>(new CompositeCell<TraceRecordProxy>(subCells));
        grid.addColumn(colMethodName, "Method");
        grid.setColumnWidth(colMethodName, 100, Style.Unit.PCT);


        Column<TraceRecordProxy, String> colTime =
            new Column<TraceRecordProxy, String>(SMALL_TEXT_CELL) {
                @Override
                public String getValue(TraceRecordProxy rec) {
                    return ClientUtil.formatDuration(rec.getTime());
                }
            };
        grid.addColumn(colTime, "Time");
        grid.setColumnWidth(colTime, 60, Style.Unit.PX);


        Column<TraceRecordProxy, String> colCalls =
            new Column<TraceRecordProxy, String>(SMALL_TEXT_CELL) {
                @Override
                public String getValue(TraceRecordProxy rec) {
                    return ""+rec.getCalls();
                }
            };
        grid.addColumn(colCalls, "Calls");
        grid.setColumnWidth(colCalls, 60, Style.Unit.PX);


        Column<TraceRecordProxy, String> colErrors =
            new Column<TraceRecordProxy, String>(SMALL_TEXT_CELL) {
                @Override
                public String getValue(TraceRecordProxy rec) {
                    return ""+rec.getErrors();
                }
            };
        grid.addColumn(colErrors, "Errors");
        grid.setColumnWidth(colErrors, 60, Style.Unit.PX);

        Column<TraceRecordProxy, String> colPct =
            new Column<TraceRecordProxy, String>(SMALL_TEXT_CELL) {
                @Override
                public String getValue(TraceRecordProxy rec) {
                    double pct = 100.0 * rec.getTime() / trace.getExecutionTime();
                    return NumberFormat.getFormat("###.0").format(pct) + "%";
                }
            };
        grid.addColumn(colPct, "Pct");
        grid.setColumnWidth(colPct, 60, Style.Unit.PX);

        rowBuilder = new TraceCallTableBuilder(grid, expandedDetails);
        grid.setTableBuilder(rowBuilder);

        // Disable hovering "features" overall to improve performance on big trees.
        grid.setSkipRowHoverStyleUpdate(true);
        grid.setSkipRowHoverFloatElementCheck(true);
        grid.setSkipRowHoverCheck(true);

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceRecordProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceRecordProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    TraceRecordProxy tr = event.getValue();
                    panelFactory.methodAttrsDialog(trace.getHostId(), trace.getDataOffs(), tr.getPath(), 0L).show();
                }
            }
        });

        grid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                //TraceRecordProxy tr = selection.getSelectedObject();
                //panelFactory.methodAttrsDialog(trace.getHostId(), trace.getDataOffs(), tr.getPath(), 0L).show();
            }
        }, DoubleClickEvent.getType());

        data = new ListDataProvider<TraceRecordProxy>();

        data.addDataDisplay(grid);

        add(grid, new VerticalLayoutData(1, 1));
    }


    private void doSearch() {
        if (searchDialog == null) {
            searchDialog = panelFactory.traceRecordSearchDialog(this, trace);
        }
        searchDialog.show();
    }


    public void setResults(List<TraceRecordProxy> results, int idx) {
        this.searchResults = results;
        goToResult(idx);
    }

    private void goToResult(final int idx) {

        // Tree has to be fully expanded in order to search results
        if (!fullyExpanded) {
            this.loadData(true, new Runnable() {
                @Override
                public void run() {
                    goToResult(idx);
                }
            });
            return;
        }

        String path = searchResults.get(idx).getPath();

        if (path.length() == 0) {
            path = "/";
        }

        List<TraceRecordProxy> lst = data.getList();

        for (int i = 0; i < lst.size(); i++) {
            TraceRecordProxy tr = lst.get(i);
            String trPath = "/"+tr.getPath();
            if (trPath.equals(path)) {
                selection.setSelected(tr, true);
                grid.getRowElement(i).scrollIntoView();
                break;
            }
        }

        curentSearchResultIdx = idx;
        btnSearchPrev.setEnabled(idx > 0);
        btnSearchNext.setEnabled(idx < searchResults.size()-1);
    }

    private void loadData(final boolean recursive, final Runnable action) {

        final PopupPanel popup = new PopupPanel();
        popup.setPopupPosition(Window.getClientWidth()/2+100, Window.getClientHeight()*1/3);
        popup.add(new Label("Loading data. Please wait ..."));
        popup.show();

        if (recursive) {
            btnExpandAll.setEnabled(false);
        }
        data.getList().clear();
        if (recursive) {
            fullyExpanded = true;
        }
        rf.traceDataService().listRecords(trace.getHostId(), trace.getDataOffs(), 0, null, recursive)
            .fire(new Receiver<List<TraceRecordProxy>>() {
                @Override
                public void onSuccess(List<TraceRecordProxy> response) {
                    data.setList(response);
                    if (action != null) {
                        action.run();
                    }
                    popup.hide();
                }
            });
    }


    private void findParent() {
        TraceRecordProxy rec = selection.getSelectedObject();

        if (rec == null) {
            return;
        }

        List<TraceRecordProxy> recList = data.getList();

        for (int idx = recList.indexOf(rec)-1; idx >= 0; idx--) {
            TraceRecordProxy prec = recList.get(idx);
            if (rec.getPath().startsWith(prec.getPath())) {
                selection.setSelected(prec, true);
                grid.getRowElement(idx).scrollIntoView();
                break;
            }
        }
    }


    private void findErrorMethod() {

        if (!fullyExpanded) {
            this.loadData(true, new Runnable() {
                @Override
                public void run() {
                    findErrorMethod();
                }
            });
            return;
        }

        TraceRecordProxy rec = selection.getSelectedObject();
        List<TraceRecordProxy> recList = data.getList();
        if (rec == null) {
            rec = recList.get(0);
        }

        for (int i = recList.indexOf(rec)+1; i < recList.size(); i++) {
            TraceRecordProxy tr = recList.get(i);
            if (tr.getExceptionInfo() != null) {
                selection.setSelected(tr, true);
                grid.getRowElement(i).scrollIntoView();
                break;
            }
        }

    }

    private void findSlowestMethod() {
        TraceRecordProxy rec = selection.getSelectedObject();
        List<TraceRecordProxy> recList = data.getList();
        if (rec == null) {
            rec = recList.get(0);
        }

        TraceRecordProxy lrec = null;
        int lidx = -1, startIdx = recList.indexOf(rec);

        if (rec.getChildren() > 0 && !isExpanded(startIdx)) {
            doExpand(rec);
            return;
        }

        if (startIdx+1 < recList.size()) {
            for (int idx = startIdx+1; idx < recList.size(); idx++) {
                TraceRecordProxy r = recList.get(idx);
                if (r.getPath().startsWith(rec.getPath())) {
                    if (lrec == null || r.getTime() > lrec.getTime()) {
                        lrec = r;
                        lidx = idx;
                    }
                } else {
                    break;
                }
            }
        } else {
            return;
        }

        if (lrec != null) {
            selection.setSelected(rec, false);
            selection.setSelected(lrec, true);
            grid.getRowElement(lidx).scrollIntoView();
            grid.getRowElement(lidx).setScrollTop(0);
            //grid.getRowElement(grid.getVisibleItems().indexOf(lrec)).scrollIntoView();
            //grid.getRowElement(grid.getVisibleItems().indexOf(lrec)).getCells().getItem(0).scrollIntoView();

            if (!isExpanded(lidx)) {
                doExpand(lrec);
            }
        }

    }

    private void doExpand(final TraceRecordProxy rec) {
        rf.traceDataService().listRecords(trace.getHostId(), trace.getDataOffs(), 0, rec.getPath(), false).fire(
                new Receiver<List<TraceRecordProxy>>() {
                    @Override
                    public void onSuccess(List<TraceRecordProxy> newrecs) {
                        List<TraceRecordProxy> list = data.getList();
                        int idx = list.indexOf(rec)+1;
                        for (int i = 0; i < newrecs.size(); i++) {
                            list.add(idx+i, newrecs.get(i));
                        }
                    }
                }
        );
    }



    private void doCollapse(TraceRecordProxy rec) {
        List<TraceRecordProxy> list = data.getList();
        int idx = list.indexOf(rec) + 1;
        while (idx < list.size() && list.get(idx).getPath().startsWith(rec.getPath())) {
            list.remove(idx);
        }
    }


    private boolean isExpanded(TraceRecordProxy rec) {
        int idx = data.getList().indexOf(rec);
        return idx >= 0 ? isExpanded(idx) : false;
    }


    private boolean isExpanded(int idx) {
        List<TraceRecordProxy> lst = data.getList();
        if (idx < lst.size()-1) {
            TraceRecordProxy tr = lst.get(idx), ntr = lst.get(idx+1);
            return tr != null && ntr != null && ntr.getPath().startsWith(tr.getPath());
        } else {
            return false;
        }
    }






    private Cell<String> SMALL_TEXT_CELL = new AbstractCell<String>() {

        @Override
        public void render(Context context, String value, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div>");
            sb.append(SafeHtmlUtils.fromString(value));
            sb.appendHtmlConstant("</div>");
        }
    };


    private final Cell<TraceRecordProxy> METHOD_NAME_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Cell.Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {

            String color = tr.getExceptionInfo() != null ? "red"
                    : tr.getAttributes() != null ? "blue" : "black";

            sb.appendHtmlConstant("<span><sp/>");
            sb.appendHtmlConstant("<span style=\"color: " + color + "; vertical-align: top; margin-top: 3px;\">");
            sb.append(SafeHtmlUtils.fromString(tr.getMethod()));
            sb.appendHtmlConstant("</span>");
            sb.appendHtmlConstant("</span>");
        }
    };



    private final Cell<TraceRecordProxy> TREE_SPACER_CELL = new AbstractCell<TraceRecordProxy>() {
        @Override
        public void render(Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
            String path = tr.getPath();
            int offs = path != null && path.length() > 0 ? (path.split("/").length) * 24 : 0;
            sb.appendHtmlConstant("<span style=\"margin-left: " + offs + "px;\"><sp></span>");
        }
    };


    String MINUS_HTML = AbstractImagePrototype.create(Resources.INSTANCE.treePlusIcon()).getHTML();
    String PLUS_HTML = AbstractImagePrototype.create(Resources.INSTANCE.treeMinusIcon()).getHTML();

    private final Cell<TraceRecordProxy> SUBTREE_EXPAND_CELL = new ActionCell<TraceRecordProxy>("",
        new ActionCell.Delegate<TraceRecordProxy>() {
                @Override
                public void execute(TraceRecordProxy rec) {
                    if (rec.getChildren() > 0) {
                        if (isExpanded(rec)) {
                            doCollapse(rec);
                        } else {
                            doExpand(rec);
                        }
                    }
                }
            }) {

        @Override
        public void render(Cell.Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
            if (tr.getChildren() > 0) {
                sb.appendHtmlConstant(isExpanded(context.getIndex()) ? MINUS_HTML : PLUS_HTML);
            }

        }
    };

    String EXPANDER_HTML = AbstractImagePrototype.create(Resources.INSTANCE.expander()).getHTML();

    private final Cell<TraceRecordProxy> DETAIL_EXPANDER_CELL = new ActionCell<TraceRecordProxy>("",
        new ActionCell.Delegate<TraceRecordProxy>() {
        @Override
        public void execute(TraceRecordProxy rec) {
            String path = rec.getPath();
            if (expandedDetails.contains(path)) {
                expandedDetails.remove(path);
            } else {
                expandedDetails.add(path);
            }
            grid.redrawRow(data.getList().indexOf(rec));
        }
    }) {
        @Override
        public void render(Cell.Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
            if ((tr.getAttributes() != null && tr.getAttributes().size() > 0)||tr.getExceptionInfo() != null) {
                sb.appendHtmlConstant(expandedDetails.contains(tr.getPath()) ? MINUS_HTML : PLUS_HTML);
                //sb.appendHtmlConstant(EXPANDER_HTML);
            }
        }
    };


    private class HasIdentityCell<T> implements HasCell<T,T> {

        private Cell<T> cell;

        public HasIdentityCell(Cell<T> cell) {
            this.cell = cell;
        }

        @Override
        public Cell getCell() {
            return cell;
        }

        @Override
        public FieldUpdater getFieldUpdater() {
            return null;
        }

        @Override
        public T getValue(T object) {
            return object;
        }
    }

}
