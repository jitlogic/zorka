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
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfoProperties;
import com.sencha.gxt.core.client.IdentityValueProvider;
import com.sencha.gxt.data.shared.TreeStore;
import com.sencha.gxt.data.shared.loader.ChildTreeStoreBinding;
import com.sencha.gxt.data.shared.loader.DataProxy;
import com.sencha.gxt.data.shared.loader.TreeLoader;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.BoxLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.RowExpander;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;
import com.sencha.gxt.widget.core.client.tree.Tree;
import com.sencha.gxt.widget.core.client.treegrid.TreeGrid;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Arrays;
import java.util.List;

public class TraceDetailPanel extends VerticalLayoutContainer {

    private final static TraceRecordInfoProperties props = GWT.create(TraceRecordInfoProperties.class);

    private TraceDataService tds;
    private TraceInfo traceInfo;
    private TreeGrid<TraceRecordInfo> methodTree;
    private TreeStore<TraceRecordInfo> methodTreeStore;


    public TraceDetailPanel(TraceDataService tds, TraceInfo traceInfo) {
        this.tds = tds;
        this.traceInfo = traceInfo;

        createToolbar();
        createTraceDetailTree();
    }


    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        TextButton btnSlowestMethod = new TextButton();
        btnSlowestMethod.setIcon(Resources.INSTANCE.goDownIcon());
        btnSlowestMethod.setToolTip("Drill down: slowest method");
        toolBar.add(btnSlowestMethod);

        btnSlowestMethod.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                findNextSlowestMethod();
            }
        });

        TextButton btnNextException = new TextButton();
        btnNextException.setIcon(Resources.INSTANCE.exceptionIcon());
        btnNextException.setToolTip("Drill down: next exception");
        toolBar.add(btnNextException);

        toolBar.add(new SeparatorToolItem());

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
        txtDuration.setWidth(100);
        txtDuration.setToolTip("Minimum trace execution time (milliseconds)");
        toolBar.add(txtDuration);

        toolBar.add(new SeparatorToolItem());

        TextButton btnExpandAll = new TextButton();
        btnExpandAll.setIcon(Resources.INSTANCE.expandIcon());
        btnExpandAll.setToolTip("Expand all");
        toolBar.add(btnExpandAll);

        btnExpandAll.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                methodTree.expandAll();
            }
        });

        toolBar.add(new SeparatorToolItem());

        TextButton btnSearchPrev = new TextButton();
        btnSearchPrev.setIcon(Resources.INSTANCE.goPrevIcon());
        btnSearchPrev.setToolTip("Search previous occurence");
        toolBar.add(btnSearchPrev);

        TextButton btnSearchNext = new TextButton();
        btnSearchNext.setIcon(Resources.INSTANCE.goNextIcon());
        btnSearchNext.setToolTip("Search next");
        toolBar.add(btnSearchNext);

        final TextField txtFilter = new TextField();
        BoxLayoutContainer.BoxLayoutData txtFilterLayout = new BoxLayoutContainer.BoxLayoutData();
        txtFilterLayout.setFlex(1.0);
        txtFilter.setToolTip("Search for text (in class/method name or attributes)");
        txtFilter.setLayoutData(txtFilterLayout);
        toolBar.add(txtFilter);

        add(toolBar, new VerticalLayoutData(1, -1));
    }


    private void findNextSlowestMethod() {
        TraceRecordInfo info = null;


        for (TraceRecordInfo i : methodTreeStore.getAll()) {
            if ((info == null || i.getTime() > info.getTime()) &&
                    (!methodTree.isExpanded(i) && !methodTree.isLeaf(i))) {
                info = i;
            }
        }

        if (info != null) {
            methodTree.setExpanded(info, true);
            methodTree.getSelectionModel().setSelection(Arrays.asList(info));
            // TODO scroll to selected row if needed
        }

    }


    private void createTraceDetailTree() {

        DataProxy<TraceRecordInfo, List<TraceRecordInfo>> proxy = new DataProxy<TraceRecordInfo, List<TraceRecordInfo>>() {
            @Override
            public void load(TraceRecordInfo parent, final Callback<List<TraceRecordInfo>, Throwable> callback) {
                tds.listTraceRecords(traceInfo.getHostId(), traceInfo.getDataOffs(), parent != null ? parent.getPath() : "",
                        new MethodCallback<List<TraceRecordInfo>>() {
                            @Override
                            public void onFailure(Method method, Throwable exception) {
                                callback.onFailure(exception);
                            }

                            @Override
                            public void onSuccess(Method method, List<TraceRecordInfo> records) {
                                callback.onSuccess(records);
                            }
                        });
            }
        };


        final TreeLoader<TraceRecordInfo> loader = new TreeLoader<TraceRecordInfo>(proxy) {
            public boolean hasChildren(TraceRecordInfo info) {
                return info.getChildren() > 0;
            }
        };

        methodTreeStore = new TreeStore<TraceRecordInfo>(props.key());
        loader.addLoadHandler(new ChildTreeStoreBinding<TraceRecordInfo>(methodTreeStore));

        ColumnConfig<TraceRecordInfo, Long> durationCol = new ColumnConfig<TraceRecordInfo, Long>(props.time(), 50, "Time");
        durationCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        durationCol.setMenuDisabled(true);
        durationCol.setSortable(false);

        durationCol.setCell(new AbstractCell<Long>() {
            @Override
            public void render(Context context, Long time, SafeHtmlBuilder sb) {
                double t = 1.0 * time / 1000000.0;
                String u = "ms";

                if (t > 1000.0) {
                    t /= 1000.0;
                    u = "s";
                }

                String strTime = t > 10 ? NumberFormat.getFormat("#####").format(t) + u
                        : NumberFormat.getFormat("###.00").format(t) + u;
                sb.appendHtmlConstant("<span>");
                sb.append(SafeHtmlUtils.fromString(strTime));
                sb.appendHtmlConstant("</span>");
            }
        });


        ColumnConfig<TraceRecordInfo, Long> callsCol = new ColumnConfig<TraceRecordInfo, Long>(props.calls(), 50, "Calls");
        callsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        callsCol.setMenuDisabled(true);
        callsCol.setSortable(false);

        ColumnConfig<TraceRecordInfo, Long> errorsCol = new ColumnConfig<TraceRecordInfo, Long>(props.errors(), 50, "Errors");
        errorsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        errorsCol.setMenuDisabled(true);
        errorsCol.setSortable(false);

        ColumnConfig<TraceRecordInfo, Long> pctCol = new ColumnConfig<TraceRecordInfo, Long>(props.time(), 50, "Pct");
        pctCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        pctCol.setMenuDisabled(true);
        pctCol.setSortable(false);

        pctCol.setCell(new AbstractCell<Long>() {
            @Override
            public void render(Context context, Long time, SafeHtmlBuilder sb) {
                double pct = 100.0 * time / traceInfo.getExecutionTime();
                String strTime = NumberFormat.getFormat("###.0").format(pct) + "%";
                sb.appendHtmlConstant("<span style=\"color: rgb(" + ((int) (pct * 2.49)) + ",0,0);\"><b>");
                sb.append(SafeHtmlUtils.fromString(strTime));
                sb.appendHtmlConstant("</b></span>");
            }
        });

        ColumnConfig<TraceRecordInfo, TraceRecordInfo> methodCol = new ColumnConfig<TraceRecordInfo, TraceRecordInfo>(
                new IdentityValueProvider<TraceRecordInfo>(), 500, "Method");

        methodCol.setMenuDisabled(true);
        methodCol.setSortable(false);

        methodCol.setCell(new AbstractCell<TraceRecordInfo>() {
            @Override
            public void render(Context context, TraceRecordInfo tr, SafeHtmlBuilder sb) {
                String color = tr.getExceptionInfo() != null ? "red"
                        : tr.getAttributes() != null ? "blue" : "black";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(tr.getMethod()));
                sb.appendHtmlConstant("</span>");
            }
        });

        RowExpander<TraceRecordInfo> expander = new RowExpander<TraceRecordInfo>(
                new IdentityValueProvider<TraceRecordInfo>(), new MethodDetailCell());

        ColumnModel<TraceRecordInfo> model = new ColumnModel<TraceRecordInfo>(
                Arrays.<ColumnConfig<TraceRecordInfo, ?>>asList(
                        expander, methodCol, durationCol, callsCol, errorsCol, pctCol));


        methodTree = new TreeGrid<TraceRecordInfo>(methodTreeStore, model, methodCol) {
            @Override
            protected void onAfterFirstAttach() {
                super.onAfterFirstAttach();
                loader.load();
            }

            protected ImageResource calculateIconStyle(TraceRecordInfo model) {
                return null;
            }
        };
        methodTree.setBorders(true);
        methodTree.setTreeLoader(loader);
        methodTree.getView().setTrackMouseOver(false);
        methodTree.getView().setAutoExpandColumn(methodCol);
        methodTree.getView().setForceFit(true);

        expander.initPlugin(methodTree);

        add(methodTree, new VerticalLayoutData(1, 1));
    }
}
