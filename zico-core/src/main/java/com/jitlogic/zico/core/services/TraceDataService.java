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
package com.jitlogic.zico.core.services;

import com.jitlogic.zico.core.*;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.FullTextTraceRecordMatcher;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zico.data.*;
import com.jitlogic.zorka.common.tracedata.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Path("hosts")
public class TraceDataService {

    private HostStoreManager storeManager;

    private TraceTypeRegistry traceTypeRegistry;

    private SymbolRegistry symbolRegistry;

    @Inject
    public TraceDataService(HostStoreManager storeManager, TraceTypeRegistry traceTypeRegistry, SymbolRegistry symbolRegistry) {
        this.storeManager = storeManager;
        this.traceTypeRegistry = traceTypeRegistry;
        this.symbolRegistry = symbolRegistry;
    }


    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HostInfo> getHosts() {
        List<HostInfo> infos = new ArrayList<HostInfo>();
        for (HostStore host : storeManager.list()) {
            infos.add(host.getHostInfo());
        }
        return infos;
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceId: [0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceInfo getTrace(@PathParam("hostId") int hostId, @PathParam("traceId") long traceOffs) {

        return storeManager.getHost(hostId).getTrace(traceOffs);
    }


    @POST
    @Path("/{hostId: [0-9]+}/page")
    @Consumes(MediaType.APPLICATION_JSON)
    public PagingData<TraceInfo> pageTraces(@PathParam("hostId") int hostId,
                                            @DefaultValue("0") @QueryParam("offset") int offset,
                                            @DefaultValue("100") @QueryParam("limit") int limit,
                                            TraceListFilterExpression filter) {

        return storeManager.getHost(hostId).pageTraces(offset, limit, filter);
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceOffs: [0-9]+}/get")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceRecordInfo getRecord(
            @PathParam("hostId") int hostId,
            @PathParam("traceOffs") long traceOffs,
            @DefaultValue("0") @QueryParam("minTime") long minTime,
            @DefaultValue("") @QueryParam("path") String path) {

        TraceRecordStore ctx = storeManager.getHost(hostId).getTraceContext(traceOffs);
        return ctx.packTraceRecord(ctx.getTraceRecord(path, minTime), path, null);
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceOffs: [0-9]+}/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TraceRecordInfo> listRecords(
            @PathParam("hostId") int hostId,
            @PathParam("traceOffs") long traceOffs,
            @DefaultValue("0") @QueryParam("minTime") long minTime,
            @DefaultValue("") @QueryParam("path") String path) {

        if ("null".equals(path)) {
            path = null;
        }

        TraceRecordStore ctx = storeManager.getHost(hostId).getTraceContext(traceOffs);
        TraceRecord tr = ctx.getTraceRecord(path, minTime);

        List<TraceRecordInfo> lst = new ArrayList<TraceRecordInfo>();

        if (path != null) {
            for (int i = 0; i < tr.numChildren(); i++) {
                lst.add(ctx.packTraceRecord(tr.getChild(i), path.length() > 0 ? (path + "/" + i) : "" + i, 250));
            }
        } else {
            lst.add(ctx.packTraceRecord(tr, "", 250));
        }

        return lst;
    }


    @POST
    @Path("/{hostId: [0-9]+}/{traceOffs: [0-9]+}/search")
    @Consumes(MediaType.APPLICATION_JSON)
    public TraceRecordSearchResult searchRecords(
            @PathParam("hostId") int hostId,
            @PathParam("traceOffs") long traceOffs,
            @DefaultValue("0") @QueryParam("minTime") long minTime,
            @DefaultValue("") @QueryParam("path") String path,
            TraceDetailSearchExpression expr) {

        TraceRecordStore ctx = storeManager.getHost(hostId).getTraceContext(traceOffs);
        TraceRecord tr = ctx.getTraceRecord(path, minTime);
        TraceRecordSearchResult result = new TraceRecordSearchResult();
        result.setResult(new ArrayList<TraceRecordInfo>());
        result.setMinTime(Long.MAX_VALUE);
        result.setMaxTime(Long.MIN_VALUE);

        TraceRecordMatcher matcher;
        String se = expr.getSearchExpr();
        switch (expr.getType()) {
            case TraceDetailSearchExpression.TXT_QUERY:
                if (se != null && se.startsWith("~")) {
                    int rflag = 0 != (expr.getFlags() & TraceDetailSearchExpression.IGNORE_CASE) ? Pattern.CASE_INSENSITIVE : 0;
                    Pattern regex = Pattern.compile(se.substring(1, se.length()), rflag);
                    matcher = new FullTextTraceRecordMatcher(symbolRegistry, expr.getFlags(), regex);
                } else {
                    matcher = new FullTextTraceRecordMatcher(symbolRegistry, expr.getFlags(), se);
                }
                break;
            case TraceDetailSearchExpression.EQL_QUERY:
                matcher = new EqlTraceRecordMatcher(symbolRegistry, Parser.expr(se), expr.getFlags(), tr.getTime());
                break;
            default:
                throw new ZicoRuntimeException("Illegal search expression type: " + expr.getType());
        }
        ctx.searchRecords(tr, path, matcher, result, tr.getTime(), false);

        if (result.getMinTime() == Long.MAX_VALUE) {
            result.setMinTime(0);
        }

        if (result.getMaxTime() == Long.MIN_VALUE) {
            result.setMaxTime(0);
        }

        return result;
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addHost(HostInfo hostInfo) {
        HostStore store = storeManager.getOrCreateHost(hostInfo.getName(), hostInfo.getAddr());
        store.updateInfo(hostInfo);
        store.save();
    }


    @PUT
    @Path("/{hostId: [0-9]+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateHost(@PathParam("hostId") int hostId, HostInfo info) {
        HostStore store = storeManager.getHost(hostId);
        store.updateInfo(info);
        store.save();
    }


    @DELETE
    @Path("/{hostId: [0-9]+}")
    public void deleteHost(@PathParam("hostId") int hostId) throws IOException {
        storeManager.delete(hostId);
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceOffs: [0-9]+}/rank")
    public List<MethodRankInfo> traceMethodRank(
            @PathParam("hostId") int hostId,
            @PathParam("traceOffs") long traceOffs,
            @DefaultValue("calls") @QueryParam("orderBy") String orderBy,
            @DefaultValue("DESC") @QueryParam("orderDesc") String orderDesc) {
        TraceRecordStore ctx = storeManager.getHost(hostId).getTraceContext(traceOffs);
        return ctx.methodRank(orderBy, orderDesc);
    }

    @GET
    @Path("/tidmap")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, String> getTidMap() {
        return traceTypeRegistry.getTidMap(null);
    }

    @GET
    @Path("/tidmap/{hostId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, String> getTidMap(@PathParam("hostId") int hostId) {
        return traceTypeRegistry.getTidMap(hostId);
    }

}
