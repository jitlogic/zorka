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


import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.props.MethodRankInfoProperties;
import com.jitlogic.zico.shared.data.MethodRankProxy;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.GridView;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class MethodRankingPanel extends VerticalLayoutContainer {

    private static final MethodRankInfoProperties props = GWT.create(MethodRankInfoProperties.class);

    private ZicoRequestFactory rf;
    private TraceInfoProxy traceInfo;
    private ErrorHandler errorHandler;

    private Grid<MethodRankProxy> rankGrid;
    private GridView<MethodRankProxy> rankGridView;
    private ListStore<MethodRankProxy> rankStore;

    private final int COL_SZ = 40;

    @Inject
    public MethodRankingPanel(ZicoRequestFactory rf, ErrorHandler errorHandler, @Assisted TraceInfoProxy traceInfo) {
        this.rf = rf;
        this.traceInfo = traceInfo;
        this.errorHandler = errorHandler;

        createRankingGrid();
        loadData("calls", "DESC");
    }

    private void createRankingGrid() {

        ColumnConfig<MethodRankProxy, String> colMethod = new ColumnConfig<MethodRankProxy, String>(props.method(), 500, "Method");
        colMethod.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colCalls = new ColumnConfig<MethodRankProxy, Long>(props.calls(), COL_SZ, "Calls");
        colCalls.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colCalls.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colErrors = new ColumnConfig<MethodRankProxy, Long>(props.errors(), COL_SZ, "Errors");
        colErrors.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colErrors.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colTime = new ColumnConfig<MethodRankProxy, Long>(props.time(), COL_SZ, "Time");
        colTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colTime.setToolTip(SafeHtmlUtils.fromString("Total execution time - sum of execution times of all method calls"));
        colTime.setCell(new NanoTimeRenderingCell());
        colTime.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colMinTime = new ColumnConfig<MethodRankProxy, Long>(props.minTime(), COL_SZ, "MinTime");
        colTime.setToolTip(SafeHtmlUtils.fromString("Peak execution time"));
        colMinTime.setCell(new NanoTimeRenderingCell());
        colMinTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colMinTime.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colMaxTime = new ColumnConfig<MethodRankProxy, Long>(props.maxTime(), COL_SZ, "MaxTime");
        colTime.setToolTip(SafeHtmlUtils.fromString("Peak execution time"));
        colMaxTime.setCell(new NanoTimeRenderingCell());
        colMaxTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colMaxTime.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colAvgTime = new ColumnConfig<MethodRankProxy, Long>(props.avgTime(), COL_SZ, "AvgTime");
        colTime.setToolTip(SafeHtmlUtils.fromString("Average execution time"));
        colAvgTime.setCell(new NanoTimeRenderingCell());
        colAvgTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colAvgTime.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colBareTime = new ColumnConfig<MethodRankProxy, Long>(props.bareTime(), COL_SZ, "BTime");
        colBareTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colBareTime.setToolTip(SafeHtmlUtils.fromString("Total bare execution time - with child methods time subtracted"));
        colBareTime.setCell(new NanoTimeRenderingCell());
        colBareTime.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colMaxBareTime = new ColumnConfig<MethodRankProxy, Long>(props.maxBareTime(), COL_SZ, "MaxBTime");
        colMaxBareTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colMaxBareTime.setToolTip(SafeHtmlUtils.fromString("Maximum bare execution time - with child methods time subtracted"));
        colMaxBareTime.setCell(new NanoTimeRenderingCell());
        colMaxBareTime.setMenuDisabled(true);

        ColumnConfig<MethodRankProxy, Long> colAvgBareTime = new ColumnConfig<MethodRankProxy, Long>(props.avgBareTime(), COL_SZ, "AvgBTime");
        colAvgBareTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colAvgBareTime.setToolTip(SafeHtmlUtils.fromString("Average bare execution time - with child methods time subtracted"));
        colAvgBareTime.setCell(new NanoTimeRenderingCell());
        colAvgBareTime.setMenuDisabled(true);

        ColumnModel<MethodRankProxy> model = new ColumnModel<MethodRankProxy>(Arrays.<ColumnConfig<MethodRankProxy, ?>>asList(
                colCalls, colErrors, colTime, colMinTime, colMaxTime, colAvgTime, colBareTime, colAvgBareTime, colMethod
        ));

        rankStore = new ListStore<MethodRankProxy>(props.key());
        rankGrid = new Grid<MethodRankProxy>(rankStore, model);
        rankGridView = rankGrid.getView();

        rankGridView.setAutoExpandColumn(colMethod);
        rankGridView.setForceFit(true);

        add(rankGrid, new VerticalLayoutData(1, 1));
    }

    private void loadData(String orderBy, String orderDir) {
        rf.traceDataService().traceMethodRank(traceInfo.getHostId(), traceInfo.getDataOffs(), orderBy, orderDir).fire(
                new Receiver<List<MethodRankProxy>>() {
                    @Override
                    public void onSuccess(List<MethodRankProxy> ranking) {
                        rankStore.clear();
                        rankStore.addAll(ranking);
                    }
                }
        );
    }
}
