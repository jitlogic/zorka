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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;

public class TraceDetailViewModel implements TreeViewModel {

    private int hostId;

    private RoofClient<RoofRecord> client = new RoofClient<RoofRecord>("roof/hosts");

    private final SingleSelectionModel<RoofRecord> selectionModel = new SingleSelectionModel<RoofRecord>();
    private final Cell<RoofRecord> cell = new TraceDetailCell();


    public TraceDetailViewModel(RoofRecord trace) {
        this.hostId = Integer.parseInt(trace.getS("HOST_ID"));
    }


    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
        RoofRecord rec = (RoofRecord)value;
        TraceDetailDataProvider provider = new TraceDetailDataProvider(client, hostId, rec);
        return new DefaultNodeInfo<RoofRecord>(provider, cell, selectionModel, null);
    }


    @Override
    public boolean isLeaf(Object value) {
        //return true;
        if (value == null) {
            return true;
        }
        int numChildren = Integer.parseInt(((RoofRecord) value).getS("CHILDREN"));
        return numChildren == 0;
    }
}
