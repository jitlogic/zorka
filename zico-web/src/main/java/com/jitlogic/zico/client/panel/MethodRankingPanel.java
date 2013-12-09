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
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.api.TraceDataApi;
import com.jitlogic.zico.data.MethodRankInfo;
import com.jitlogic.zico.data.MethodRankInfoProperties;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.GridView;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class MethodRankingPanel extends VerticalLayoutContainer {

    private static final MethodRankInfoProperties props = GWT.create(MethodRankInfoProperties.class);

    private TraceDataApi api;
    private TraceInfoProxy traceInfo;
    private ErrorHandler errorHandler;

    private Grid<MethodRankInfo> rankGrid;
    private GridView<MethodRankInfo> rankGridView;
    private ListStore<MethodRankInfo> rankStore;

    private final int COL_SZ = 40;

    @Inject
    public MethodRankingPanel(TraceDataApi api, ErrorHandler errorHandler, @Assisted TraceInfoProxy traceInfo) {
        this.api = api;
        this.traceInfo = traceInfo;
        this.errorHandler = errorHandler;

        createRankingGrid();
        loadData("calls", "DESC");
    }

    private void createRankingGrid() {

        ColumnConfig<MethodRankInfo, String> colMethod = new ColumnConfig<MethodRankInfo, String>(props.method(), 500, "Method");
        colMethod.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colCalls = new ColumnConfig<MethodRankInfo, Long>(props.calls(), COL_SZ, "Calls");
        colCalls.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colCalls.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colErrors = new ColumnConfig<MethodRankInfo, Long>(props.errors(), COL_SZ, "Errors");
        colErrors.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colErrors.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colTime = new ColumnConfig<MethodRankInfo, Long>(props.time(), COL_SZ, "Time");
        colTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colTime.setToolTip(SafeHtmlUtils.fromString("Total execution time - sum of execution times of all method calls"));
        colTime.setCell(new NanoTimeRenderingCell());
        colTime.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colMinTime = new ColumnConfig<MethodRankInfo, Long>(props.minTime(), COL_SZ, "MinTime");
        colTime.setToolTip(SafeHtmlUtils.fromString("Peak execution time"));
        colMinTime.setCell(new NanoTimeRenderingCell());
        colMinTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colMinTime.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colMaxTime = new ColumnConfig<MethodRankInfo, Long>(props.maxTime(), COL_SZ, "MaxTime");
        colTime.setToolTip(SafeHtmlUtils.fromString("Peak execution time"));
        colMaxTime.setCell(new NanoTimeRenderingCell());
        colMaxTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colMaxTime.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colAvgTime = new ColumnConfig<MethodRankInfo, Long>(props.avgTime(), COL_SZ, "AvgTime");
        colTime.setToolTip(SafeHtmlUtils.fromString("Average execution time"));
        colAvgTime.setCell(new NanoTimeRenderingCell());
        colAvgTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colAvgTime.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colBareTime = new ColumnConfig<MethodRankInfo, Long>(props.bareTime(), COL_SZ, "BTime");
        colBareTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colBareTime.setToolTip(SafeHtmlUtils.fromString("Total bare execution time - with child methods time subtracted"));
        colBareTime.setCell(new NanoTimeRenderingCell());
        colBareTime.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colMaxBareTime = new ColumnConfig<MethodRankInfo, Long>(props.maxBareTime(), COL_SZ, "MaxBTime");
        colMaxBareTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colMaxBareTime.setToolTip(SafeHtmlUtils.fromString("Maximum bare execution time - with child methods time subtracted"));
        colMaxBareTime.setCell(new NanoTimeRenderingCell());
        colMaxBareTime.setMenuDisabled(true);

        ColumnConfig<MethodRankInfo, Long> colAvgBareTime = new ColumnConfig<MethodRankInfo, Long>(props.avgBareTime(), COL_SZ, "AvgBTime");
        colAvgBareTime.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        colAvgBareTime.setToolTip(SafeHtmlUtils.fromString("Average bare execution time - with child methods time subtracted"));
        colAvgBareTime.setCell(new NanoTimeRenderingCell());
        colAvgBareTime.setMenuDisabled(true);

        ColumnModel<MethodRankInfo> model = new ColumnModel<MethodRankInfo>(Arrays.<ColumnConfig<MethodRankInfo, ?>>asList(
                colCalls, colErrors, colTime, colMinTime, colMaxTime, colAvgTime, colBareTime, colAvgBareTime, colMethod
        ));

        rankStore = new ListStore<MethodRankInfo>(props.key());
        rankGrid = new Grid<MethodRankInfo>(rankStore, model);
        rankGridView = rankGrid.getView();

        rankGridView.setAutoExpandColumn(colMethod);
        rankGridView.setForceFit(true);

        add(rankGrid, new VerticalLayoutData(1, 1));
    }

    private void loadData(String orderBy, String orderDir) {
        api.traceMethodRank(traceInfo.getHostId(), traceInfo.getDataOffs(), orderBy, orderDir,
                new MethodCallback<List<MethodRankInfo>>() {
                    @Override
                    public void onFailure(Method method, Throwable exception) {
                        errorHandler.error("Error calling " + method, exception);
                    }

                    @Override
                    public void onSuccess(Method method, List<MethodRankInfo> ranking) {
                        rankStore.clear();
                        rankStore.addAll(ranking);
                    }
                });
    }
}
