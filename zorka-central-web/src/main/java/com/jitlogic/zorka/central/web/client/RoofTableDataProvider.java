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
import java.util.Map;

public class RoofTableDataProvider extends AsyncDataProvider<RoofRecord> {

    private RoofClient<RoofRecord> client;
    private String hostIdStr;

    public RoofTableDataProvider(RoofClient<RoofRecord> client) {
        this.client = client;
    }

    public void setHostId(String hostIdStr) {
        this.hostIdStr = hostIdStr;

        client.call(hostIdStr + "/collections/traces", "count", new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Error", caught);
                updateRowCount(0, true);
            }

            @Override
            public void onSuccess(String result) {
                GWT.log("Number of rows found: " + result);
                int i = Integer.parseInt(result);
                updateRowCount(i, true);
                for (HasData<RoofRecord> display : getDataDisplays()) {
                    onRangeChanged(display);
                }
            }
        });
    }

    @Override
    protected void onRangeChanged(HasData<RoofRecord> display) {

        final Range range = display.getVisibleRange();

        if (hostIdStr == null) {
            updateRowData(range.getStart(), new ArrayList<RoofRecord>());
            updateRowCount(0, true);
            return;
        }

        Map<String,String> params = new HashMap<String, String>();
        params.put("offset", ""+range.getStart());
        params.put("limit", ""+range.getLength());

        client.list(hostIdStr + "/collections/traces", params, new AsyncCallback<JsArray<RoofRecord>>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Error listing traces for " + hostIdStr, caught);
            }

            @Override
            public void onSuccess(JsArray<RoofRecord> result) {
                List<RoofRecord> records = new ArrayList<RoofRecord>();
                for (int i = 0; i < result.length(); i++) {
                    records.add(result.get(i));
                }
                updateRowData(range.getStart(), records);
            }
        });
    }
}
