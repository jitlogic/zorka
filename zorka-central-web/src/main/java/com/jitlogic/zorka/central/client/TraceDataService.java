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


import com.jitlogic.zorka.central.data.*;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.*;

import java.io.IOException;
import java.util.List;

public interface TraceDataService extends RestService {

    @GET
    @Path("hosts/list")
    public void listHosts(MethodCallback<List<HostInfo>> callback);


    @POST
    @Path("hosts/{hostId}/page?limit={limit}&offset={offset}")
    public void pageTraces(@PathParam("hostId") int hostId,
                           @PathParam("offset") int offset,
                           @PathParam("limit") int limit,
                           TraceListFilterExpression filter,
                           MethodCallback<PagingData<TraceInfo>> callback);


    @GET
    @Path("hosts/{hostId}/{traceOffs}/list?path={path}&minTime={minTime}")
    public void listTraceRecords(@PathParam("hostId") int hostId,
                                 @PathParam("traceOffs") long traceOffs,
                                 @PathParam("minTime") long minTime,
                                 @PathParam("path") String path,
                                 MethodCallback<List<TraceRecordInfo>> callback);


    @GET
    @Path("hosts/{hostId}/{traceOffs}/get?path={path}&minTime={minTime}")
    public void getTraceRecord(@PathParam("hostId") int hostId,
                               @PathParam("traceOffs") long traceOffs,
                               @PathParam("minTime") long minTime,
                               @PathParam("path") String path,
                               MethodCallback<TraceRecordInfo> callback);


    @POST
    @Path("hosts/")
    public void addHost(HostInfo hostInfo,
                        MethodCallback<Void> callback);


    @PUT
    @Path("hosts/{hostId}")
    public void updateHost(@PathParam("hostId") int hostId,
                           HostInfo hostIndo,
                           MethodCallback<Void> callback);


    @DELETE
    @Path("hosts/{hostId}")
    public void deleteHost(@PathParam("hostId") int hostId,
                           MethodCallback<Void> callback);

}
