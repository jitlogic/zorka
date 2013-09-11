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
package com.jitlogic.zorka.central.client.panels;


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.jitlogic.zorka.central.client.api.TraceDataApi;
import com.jitlogic.zorka.central.data.TraceDetailSearchExpression;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfoProperties;
import com.sencha.gxt.core.client.IdentityValueProvider;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.CheckBox;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.RowExpander;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Arrays;
import java.util.List;

public class TraceRecordSearchDialog extends Dialog {

    private final static TraceRecordInfoProperties props = GWT.create(TraceRecordInfoProperties.class);

    private TraceDataApi tds;
    private TraceInfo trace;
    private TraceRecordInfo root;

    private TraceDetailPanel panel;

    private TraceDetailSearchExpression expr = new TraceDetailSearchExpression();

    private ListStore<TraceRecordInfo> resultsStore;
    private Grid<TraceRecordInfo> resultsGrid;

    private TextField txtSearchFilter;
    private CheckBox chkClass;
    private CheckBox chkMethod;
    private CheckBox chkAttribs;
    private CheckBox chkExceptionText;
    private CheckBox chkErrorsOnly;
    private CheckBox chkMethodsWithAttrs;

    public TraceRecordSearchDialog(TraceDetailPanel panel, TraceDataApi tds,
                                   TraceInfo trace, TraceRecordInfo root) {
        this.tds = tds;
        this.trace = trace;
        this.root = root;
        this.panel = panel;

        createUI();
    }

    private void createUI() {

        setHeadingText("Search for methods ...");
        setPredefinedButtons();
        setPixelSize(1200, 850);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();

        VerticalLayoutContainer.VerticalLayoutData vd = new VerticalLayoutContainer.VerticalLayoutData();
        vd.setMargins(new Margins(10, 0, 0, 0));

        vp.setLayoutData(vd);

        txtSearchFilter = new TextField();
        txtSearchFilter.setEmptyText("Enter search text ...");
        //vp.add(txtSearchFilter);
        vp.add(new FieldLabel(txtSearchFilter, "Search filter"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        HorizontalPanel hp = new HorizontalPanel();

        chkClass = new CheckBox();
        chkClass.setBoxLabel("Class names");
        chkClass.setValue(true);
        hp.add(chkClass);

        chkMethod = new CheckBox();
        chkMethod.setBoxLabel("Method names");
        chkMethod.setValue(true);
        hp.add(chkMethod);

        chkAttribs = new CheckBox();
        chkAttribs.setBoxLabel("Attributes");
        chkAttribs.setValue(true);
        hp.add(chkAttribs);

        chkExceptionText = new CheckBox();
        chkExceptionText.setBoxLabel("Exception text");
        chkExceptionText.setValue(true);
        hp.add(chkExceptionText);

        vp.add(new FieldLabel(hp, "Search in"));

        HorizontalPanel hpSearchOnly = new HorizontalPanel();

        chkErrorsOnly = new CheckBox();
        chkErrorsOnly.setBoxLabel("Errors and Exceptions");
        chkErrorsOnly.setValue(false);
        hpSearchOnly.add(chkErrorsOnly);

        chkMethodsWithAttrs = new CheckBox();
        chkMethodsWithAttrs.setBoxLabel("Methods with attributes");
        chkMethodsWithAttrs.setValue(false);
        hpSearchOnly.add(chkMethodsWithAttrs);

        vp.add(new FieldLabel(hpSearchOnly, "Only in"));

        createResultsGrid();

        vp.add(resultsGrid, new VerticalLayoutContainer.VerticalLayoutData(1, 1));

        add(vp);

        TextButton btnSearch = new TextButton("Search");
        addButton(btnSearch);

        btnSearch.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                doSearch();
            }
        });

        TextButton btnFilter = new TextButton("Go to ...");
        addButton(btnFilter);

        btnFilter.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                doGoTo();
            }
        });

        TextButton btnClose = new TextButton("Close");
        btnClose.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                TraceRecordSearchDialog.this.hide();
            }
        });
        addButton(btnClose);
    }

    private void doGoTo() {
        TraceRecordInfo tri = resultsGrid.getSelectionModel().getSelectedItem();
        int idx = tri != null ? resultsStore.indexOf(tri) : 0;
        panel.setResults(resultsStore.getAll(), idx);
        this.hide();
    }

    private void createResultsGrid() {

        ColumnConfig<TraceRecordInfo, Long> durationCol = new ColumnConfig<TraceRecordInfo, Long>(props.time(), 50, "Time");
        durationCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        durationCol.setMenuDisabled(true);
        durationCol.setSortable(false);

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
                double pct = 100.0 * time / trace.getExecutionTime();
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


        resultsStore = new ListStore<TraceRecordInfo>(props.key());
        resultsGrid = new Grid<TraceRecordInfo>(resultsStore, model);

        resultsGrid.setBorders(true);
        resultsGrid.getView().setAutoExpandColumn(methodCol);
        resultsGrid.getView().setForceFit(true);

        expander.initPlugin(resultsGrid);

        resultsGrid.addCellDoubleClickHandler(new CellDoubleClickEvent.CellDoubleClickHandler() {
            @Override
            public void onCellClick(CellDoubleClickEvent event) {
                doGoTo();
            }
        });

    }

    private void doSearch() {

        expr.setFlags(
                (chkErrorsOnly.getValue() ? TraceDetailSearchExpression.ERRORS_ONLY : 0)
                        | (chkMethodsWithAttrs.getValue() ? TraceDetailSearchExpression.METHODS_WITH_ATTRS : 0)
                        | (chkClass.getValue() ? TraceDetailSearchExpression.SEARCH_CLASSES : 0)
                        | (chkMethod.getValue() ? TraceDetailSearchExpression.SEARCH_METHODS : 0)
                        | (chkAttribs.getValue() ? TraceDetailSearchExpression.SEARCH_ATTRS : 0)
                        | (chkExceptionText.getValue() ? TraceDetailSearchExpression.SEARCH_EX_MSG : 0));

        expr.setSearchExpr(txtSearchFilter.getValue());

        tds.searchTraceRecords(trace.getHostId(), trace.getDataOffs(), 0, "", expr,
                new MethodCallback<List<TraceRecordInfo>>() {
                    @Override
                    public void onFailure(Method method, Throwable exception) {
                        GWT.log("Error: ", exception);
                    }

                    @Override
                    public void onSuccess(Method method, List<TraceRecordInfo> response) {
                        resultsStore.clear();
                        resultsStore.addAll(response);
                    }
                });
    }
}
