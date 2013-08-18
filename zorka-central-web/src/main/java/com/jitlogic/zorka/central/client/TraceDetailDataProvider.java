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


import com.google.gwt.core.client.GWT;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.jitlogic.zorka.central.client.data.TraceDataService;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.List;

public class TraceDetailDataProvider extends AsyncDataProvider<TraceRecordInfo> {

    private TraceInfo traceInfo;
    private TraceRecordInfo recordInfo;
    private TraceDataService service;

    public TraceDetailDataProvider(TraceDataService service, TraceInfo traceInfo, TraceRecordInfo recordInfo) {
        super(null);   // TODO ProvidesKey ?
        this.service = service;
        this.traceInfo = traceInfo;
        this.recordInfo = recordInfo;
    }

    @Override
    public void addDataDisplay(HasData<TraceRecordInfo> display) {
        super.addDataDisplay(display);
        updateRowCount(recordInfo.getChildren(), true);
    }

    @Override
    protected void onRangeChanged(HasData<TraceRecordInfo> display) {
        final Range range = display.getVisibleRange();

        service.listTraceRecords(traceInfo.getHostId(), traceInfo.getDataOffs(), recordInfo.getPath(),
                new MethodCallback<List<TraceRecordInfo>>() {
                    @Override
                    public void onFailure(Method method, Throwable exception) {
                        GWT.log("Error calling method " + method, exception);
                    }

                    @Override
                    public void onSuccess(Method method, List<TraceRecordInfo> response) {
                        updateRowData(0, response);
                    }
                });
    }
}
