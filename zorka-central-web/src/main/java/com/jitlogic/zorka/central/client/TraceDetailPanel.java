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
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfoProperties;
import com.sencha.gxt.data.shared.TreeStore;
import com.sencha.gxt.data.shared.loader.ChildTreeStoreBinding;
import com.sencha.gxt.data.shared.loader.DataProxy;
import com.sencha.gxt.data.shared.loader.TreeLoader;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.treegrid.TreeGrid;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Arrays;
import java.util.List;

public class TraceDetailPanel extends VerticalLayoutContainer {

    private final static TraceRecordInfoProperties props = GWT.create(TraceRecordInfoProperties.class);

    private TraceDataService tds;
    private TraceInfo traceInfo;

    public TraceDetailPanel(TraceDataService tds, TraceInfo traceInfo) {
        this.tds = tds;
        this.traceInfo = traceInfo;
        createTraceDetailTree();
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

        TreeStore<TraceRecordInfo> store = new TreeStore<TraceRecordInfo>(props.key());
        loader.addLoadHandler(new ChildTreeStoreBinding<TraceRecordInfo>(store));

        ColumnConfig<TraceRecordInfo, Long> durationCol = new ColumnConfig<TraceRecordInfo, Long>(props.time(), 50, "Time");

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
        ColumnConfig<TraceRecordInfo, Long> errorsCol = new ColumnConfig<TraceRecordInfo, Long>(props.errors(), 50, "Errors");
        ColumnConfig<TraceRecordInfo, Long> pctCol = new ColumnConfig<TraceRecordInfo, Long>(props.time(), 50, "Pct");

        pctCol.setCell(new AbstractCell<Long>() {
            @Override
            public void render(Context context, Long time, SafeHtmlBuilder sb) {
                double pct = 100.0 * time / traceInfo.getExecutionTime();
                String strTime = NumberFormat.getFormat("###.0").format(pct) + "%";
                sb.appendHtmlConstant("<span>");
                sb.append(SafeHtmlUtils.fromString(strTime));
                sb.appendHtmlConstant("</span>");
            }
        });

        ColumnConfig<TraceRecordInfo, String> methodCol = new ColumnConfig<TraceRecordInfo, String>(props.method(), 500, "Method");

        ColumnModel<TraceRecordInfo> model = new ColumnModel<TraceRecordInfo>(
                Arrays.<ColumnConfig<TraceRecordInfo, ?>>asList(durationCol, callsCol, errorsCol, pctCol, methodCol));

        TreeGrid<TraceRecordInfo> tree = new TreeGrid<TraceRecordInfo>(store, model, methodCol) {
            @Override
            protected void onAfterFirstAttach() {
                super.onAfterFirstAttach();
                loader.load();
            }
        };
        tree.setBorders(true);
        tree.setTreeLoader(loader);
        tree.getView().setTrackMouseOver(false);
        tree.getView().setAutoExpandColumn(methodCol);
        tree.getView().setForceFit(true);

        add(tree, new VerticalLayoutData(1, 1));
    }
}
