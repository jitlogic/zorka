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
import com.jitlogic.zorka.central.client.data.TraceInfo;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.List;

public class TraceListDataProvider extends AsyncDataProvider<TraceInfo> {

    private int hostId;
    private TraceDataService service;


    public TraceListDataProvider(TraceDataService service) {
        this.service = service;
    }


    public void setHostId(int hostId) {
        this.hostId = hostId;
        service.countTraces(hostId, new MethodCallback<Integer>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling method " + method, exception);
            }

            @Override
            public void onSuccess(Method method, Integer numTraces) {
                GWT.log("Number of rows found: " + numTraces);
                updateRowCount(numTraces, true);
                for (HasData<TraceInfo> display : getDataDisplays()) {
                    onRangeChanged(display);
                }
            }
        });
    }


    @Override
    protected void onRangeChanged(HasData<TraceInfo> display) {
        final Range range = display.getVisibleRange();

        if (hostId > 0) {
            service.listTraces(hostId, range.getStart(), range.getLength(),
                    new MethodCallback<List<TraceInfo>>() {
                        @Override
                        public void onFailure(Method method, Throwable exception) {
                            GWT.log("Error calling method " + method, exception);
                        }

                        @Override
                        public void onSuccess(Method method, List<TraceInfo> response) {
                            updateRowData(range.getStart(), response);
                        }
                    });
        }
    }
}
