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


import com.jitlogic.zorka.central.data.HostInfo;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import java.util.List;

public interface TraceDataService extends RestService {

    @GET
    @Path("hosts/list")
    public void listHosts(MethodCallback<List<HostInfo>> callback);


    @GET
    @Path("hosts/{hostId}/list?limit={limit}&offset={offset}")
    public void listTraces(@PathParam("hostId") int hostId,
                           @PathParam("offset") int offset,
                           @PathParam("limit") int limit,
                           MethodCallback<List<TraceInfo>> callback);


    @GET
    @Path("hosts/{hostId}/count")
    public void countTraces(@PathParam("hostId") int hostId,
                            MethodCallback<Integer> callback);


    @GET
    @Path("hosts/{hostId}/{traceOffs}/list?path={path}")
    public void listTraceRecords(@PathParam("hostId") int hostId,
                                 @PathParam("traceOffs") long traceOffs,
                                 @PathParam("path") String path,
                                 MethodCallback<List<TraceRecordInfo>> callback);


    @GET
    @Path("hosts/{hostId}/{traceOffs}/get?path={path}")
    public void getTraceRecord(@PathParam("hostId") int hostId,
                               @PathParam("traceOffs") long traceOffs,
                               @PathParam("path") String path,
                               MethodCallback<TraceRecordInfo> callback);
}
