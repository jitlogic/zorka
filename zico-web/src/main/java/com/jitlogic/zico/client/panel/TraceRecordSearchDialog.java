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
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.props.TraceRecordInfoProperties;
import com.jitlogic.zico.core.model.TraceRecordSearchQuery;
import com.jitlogic.zico.shared.data.TraceRecordSearchQueryProxy;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;
import com.jitlogic.zico.shared.data.TraceRecordSearchResultProxy;
import com.jitlogic.zico.shared.services.TraceDataServiceProxy;
import com.sencha.gxt.core.client.IdentityValueProvider;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.button.ToggleButton;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.event.ShowEvent;
import com.sencha.gxt.widget.core.client.form.CheckBox;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.RowExpander;

import javax.inject.Inject;
import java.util.Arrays;

public class TraceRecordSearchDialog extends Dialog {

    private final static TraceRecordInfoProperties props = GWT.create(TraceRecordInfoProperties.class);


    private TraceInfoProxy trace;
    private String rootPath = "";

    private TraceCallTreePanel panel;
    private ZicoRequestFactory rf;

    private ListStore<TraceRecordProxy> resultsStore;
    private Grid<TraceRecordProxy> resultsGrid;

    private TextField txtSearchFilter;
    private CheckBox chkClass;
    private CheckBox chkMethod;
    private CheckBox chkAttribs;
    private CheckBox chkExceptionText;
    private CheckBox chkErrorsOnly;
    private CheckBox chkMethodsWithAttrs;
    private CheckBox chkIgnoreCase;

    private ToggleButton btnEql;

    private ErrorHandler errorHandler;

    private Label lblSumStats;


    @Inject
    public TraceRecordSearchDialog(ZicoRequestFactory rf, ErrorHandler errorHandler,
                                   @Assisted TraceCallTreePanel panel, @Assisted TraceInfoProxy trace) {

        this.rf = rf;
        this.trace = trace;
        this.panel = panel;
        this.errorHandler = errorHandler;

        createUI();
    }


    private void createUI() {

        setHeadingText("Search for methods");
        setPredefinedButtons();
        setPixelSize(1200, 750);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();

        VerticalLayoutContainer.VerticalLayoutData vd = new VerticalLayoutContainer.VerticalLayoutData();
        vd.setMargins(new Margins(10, 0, 0, 0));

        vp.setLayoutData(vd);

        HorizontalLayoutContainer hl1 = new HorizontalLayoutContainer();
        HorizontalLayoutContainer.HorizontalLayoutData hd1 = new HorizontalLayoutContainer.HorizontalLayoutData();
        hd1.setMargins(new Margins(0, 4, 4, 0));
        hl1.setLayoutData(hd1);

        btnEql = new ToggleButton();
        btnEql.setIcon(Resources.INSTANCE.eqlIcon());
        btnEql.setToolTip("EQL query (instead of full text search)");
        hl1.add(btnEql);

        txtSearchFilter = new TextField();
        txtSearchFilter.setEmptyText("Enter search text ...");
        hl1.add(txtSearchFilter, new HorizontalLayoutContainer.HorizontalLayoutData(1, 1));

        txtSearchFilter.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    doSearch();
                }
            }
        });

        TextButton btnSearch = new TextButton();
        btnSearch.setIcon(Resources.INSTANCE.searchIcon());
        btnSearch.setToolTip("Run search");
        btnSearch.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                doSearch();
            }
        });
        hl1.add(btnSearch, new HorizontalLayoutContainer.HorizontalLayoutData(-1, -1));

        TextButton btnFilter = new TextButton();
        btnFilter.setIcon(Resources.INSTANCE.gotoIcon());
        btnFilter.setToolTip("Go to ...");
        btnFilter.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                doGoTo();
            }
        });
        hl1.add(btnFilter, new HorizontalLayoutContainer.HorizontalLayoutData(-1, -1));

        vp.add(new FieldLabel(hl1, "Search filter"),
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
        hp.add(chkExceptionText);

        vp.add(new FieldLabel(hp, "Search in"));

        chkErrorsOnly = new CheckBox();
        chkErrorsOnly.setBoxLabel("Only Errors and Exceptions");
        chkErrorsOnly.setValue(false);
        hp.add(chkErrorsOnly);

        chkMethodsWithAttrs = new CheckBox();
        chkMethodsWithAttrs.setBoxLabel("Only Methods with attributes");
        chkMethodsWithAttrs.setValue(false);
        hp.add(chkMethodsWithAttrs);

        chkIgnoreCase = new CheckBox();
        chkIgnoreCase.setBoxLabel("Ignore Case");
        chkIgnoreCase.setValue(true);
        hp.add(chkIgnoreCase);

        lblSumStats = new Label("n/a");

        vp.add(new FieldLabel(lblSumStats, "Summary"));

        createResultsGrid();

        vp.add(resultsGrid, new VerticalLayoutContainer.VerticalLayoutData(1, 1));

        add(vp);

        addShowHandler(new ShowEvent.ShowHandler() {
            @Override
            public void onShow(ShowEvent event) {
                setFocusWidget(txtSearchFilter);
            }
        });
    }


    private void doGoTo() {
        TraceRecordProxy tri = resultsGrid.getSelectionModel().getSelectedItem();
        int idx = tri != null ? resultsStore.indexOf(tri) : 0;
        panel.setResults(resultsStore.getAll(), idx);
        this.hide();
    }


    public void setRootPath(String rootPath) {
        if (this.rootPath != rootPath) {
            this.rootPath = rootPath;
            this.resultsStore.clear();
            this.lblSumStats.setText("n/a");
        }
    }


    private void createResultsGrid() {

        ColumnConfig<TraceRecordProxy, Long> durationCol = new ColumnConfig<TraceRecordProxy, Long>(props.time(), 50, "Time");
        durationCol.setCell(new NanoTimeRenderingCell());
        durationCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        durationCol.setMenuDisabled(true);

        ColumnConfig<TraceRecordProxy, Long> callsCol = new ColumnConfig<TraceRecordProxy, Long>(props.calls(), 50, "Calls");
        callsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        callsCol.setMenuDisabled(true);

        ColumnConfig<TraceRecordProxy, Long> errorsCol = new ColumnConfig<TraceRecordProxy, Long>(props.errors(), 50, "Errors");
        errorsCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        errorsCol.setMenuDisabled(true);

        ColumnConfig<TraceRecordProxy, Long> pctCol = new ColumnConfig<TraceRecordProxy, Long>(props.time(), 50, "Pct");
        pctCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        pctCol.setMenuDisabled(true);

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

        ColumnConfig<TraceRecordProxy, TraceRecordProxy> methodCol = new ColumnConfig<TraceRecordProxy, TraceRecordProxy>(
                new IdentityValueProvider<TraceRecordProxy>(), 500, "Method");

        methodCol.setMenuDisabled(true);
        methodCol.setSortable(false);

        methodCol.setCell(new AbstractCell<TraceRecordProxy>() {
            @Override
            public void render(Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
                String color = tr.getExceptionInfo() != null ? "red"
                        : tr.getAttributes() != null ? "blue" : "black";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(tr.getMethod()));
                sb.appendHtmlConstant("</span>");
            }
        });

        RowExpander<TraceRecordProxy> expander = new RowExpander<TraceRecordProxy>(
                new IdentityValueProvider<TraceRecordProxy>(), new MethodDetailCell());

        ColumnModel<TraceRecordProxy> model = new ColumnModel<TraceRecordProxy>(
                Arrays.<ColumnConfig<TraceRecordProxy, ?>>asList(
                        expander, methodCol, durationCol, callsCol, errorsCol, pctCol));


        resultsStore = new ListStore<TraceRecordProxy>(props.key());
        resultsGrid = new Grid<TraceRecordProxy>(resultsStore, model);

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
        TraceDataServiceProxy req = rf.traceDataService();
        TraceRecordSearchQueryProxy expr = req.create(TraceRecordSearchQueryProxy.class);

        expr.setType(btnEql.getValue() ? TraceRecordSearchQuery.EQL_QUERY : TraceRecordSearchQuery.TXT_QUERY);

        expr.setFlags(
                (chkErrorsOnly.getValue() ? TraceRecordSearchQuery.ERRORS_ONLY : 0)
                        | (chkMethodsWithAttrs.getValue() ? TraceRecordSearchQuery.METHODS_WITH_ATTRS : 0)
                        | (chkClass.getValue() ? TraceRecordSearchQuery.SEARCH_CLASSES : 0)
                        | (chkMethod.getValue() ? TraceRecordSearchQuery.SEARCH_METHODS : 0)
                        | (chkAttribs.getValue() ? TraceRecordSearchQuery.SEARCH_ATTRS : 0)
                        | (chkExceptionText.getValue() ? TraceRecordSearchQuery.SEARCH_EX_MSG : 0)
                        | (chkIgnoreCase.getValue() ? TraceRecordSearchQuery.IGNORE_CASE : 0));

        expr.setSearchExpr(txtSearchFilter.getCurrentValue());

        req.searchRecords(trace.getHostName(), trace.getDataOffs(), 0, rootPath, expr).fire(
                new Receiver<TraceRecordSearchResultProxy>() {
                    @Override
                    public void onSuccess(TraceRecordSearchResultProxy response) {
                        resultsStore.clear();
                        resultsStore.addAll(response.getResult());
                        lblSumStats.setText(response.getResult().size() + " methods, "
                                + NumberFormat.getFormat("###.0").format(response.getRecurPct()) + "% of trace execution time. "
                                + "Time: " + ClientUtil.formatDuration(response.getRecurTime()) + " non-recursive"
                                + ", " + ClientUtil.formatDuration(response.getMinTime()) + " min, "
                                + ", " + ClientUtil.formatDuration(response.getMaxTime()) + " max."

                        );
                        setFocusWidget(txtSearchFilter);
                    }
                }
        );
    }
}
