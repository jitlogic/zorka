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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;

public class TraceDetailViewModel implements TreeViewModel {

    private TraceInfo traceInfo;
    private TraceDataService service;

    private final SingleSelectionModel<TraceRecordInfo> selectionModel = new SingleSelectionModel<TraceRecordInfo>(
            new ProvidesKey<TraceRecordInfo>() {
                @Override
                public Object getKey(TraceRecordInfo item) {
                    return item.getPath();
                }
            }
    );
    private final Cell<TraceRecordInfo> cell = new TraceDetailCell();


    public TraceDetailViewModel(TraceDataService service, TraceInfo traceInfo) {
        this.traceInfo = traceInfo;
        this.service = service;
    }


    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
        TraceRecordInfo recordInfo = (TraceRecordInfo) value;
        TraceDetailDataProvider provider = new TraceDetailDataProvider(service, traceInfo, recordInfo);
        return new DefaultNodeInfo<TraceRecordInfo>(provider, cell, selectionModel, null);
    }


    @Override
    public boolean isLeaf(Object value) {
        return value != null ? ((TraceRecordInfo) value).getChildren() == 0 : true;
    }
}

