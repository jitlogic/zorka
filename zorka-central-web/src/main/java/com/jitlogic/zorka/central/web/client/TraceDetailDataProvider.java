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
package com.jitlogic.zorka.central.web.client;


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TraceDetailDataProvider extends AsyncDataProvider<RoofRecord> {

    private String path;
    private int children;

    private int hostId;
    private int traceOffs;

    private RoofClient<RoofRecord> client;

    public TraceDetailDataProvider(RoofClient<RoofRecord> client, int hostId, RoofRecord record) {
        super(null);
        this.client = client;
        this.hostId = hostId;
        this.path = record.getS("PATH");
        this.children = Integer.parseInt(record.getS("CHILDREN"));
    }

    @Override
    public void addDataDisplay(HasData<RoofRecord> display) {
        super.addDataDisplay(display);
        updateRowCount(children, true);
    }

    @Override
    protected void onRangeChanged(HasData<RoofRecord> display) {
        final Range range = display.getVisibleRange();
        client.callL("" + hostId + "/collections/traces/" + traceOffs , "listRecords",
                new HashMap<String, String>(), new AsyncCallback<JsArray<RoofRecord>>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Error: ", caught);
            }

            @Override
            public void onSuccess(JsArray<RoofRecord> result) {
                List<RoofRecord> lst = new ArrayList<RoofRecord>();
                for (int i = 0; i < result.length(); i++) {
                    lst.add(result.get(i));
                }
                updateRowData(0, lst);
            }
        });
    }
}
