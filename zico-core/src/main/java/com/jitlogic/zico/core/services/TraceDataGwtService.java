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

import com.google.inject.Singleton;
import com.jitlogic.zico.core.*;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.model.PagingData;
import com.jitlogic.zico.core.model.TraceInfo;
import com.jitlogic.zico.core.model.TraceListFilterExpression;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.FullTextTraceRecordMatcher;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zico.core.model.MethodRankInfo;
import com.jitlogic.zico.core.model.TraceDetailSearchExpression;
import com.jitlogic.zico.core.model.TraceRecordInfo;
import com.jitlogic.zico.core.model.TraceRecordSearchResult;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class TraceDataGwtService {
    private HostStoreManager storeManager;
    private TraceTypeRegistry traceTypeRegistry;
    private SymbolRegistry symbolRegistry;
    private UserManager userManager;


    @Inject
    public TraceDataGwtService(HostStoreManager storeManager, TraceTypeRegistry traceTypeRegistry,
                            SymbolRegistry symbolRegistry, UserManager userManager) {
        this.storeManager = storeManager;
        this.traceTypeRegistry = traceTypeRegistry;
        this.symbolRegistry = symbolRegistry;
        this.userManager = userManager;
    }


    public TraceInfo getTrace(int hostId, long traceOffs) {

        return storeManager.getHost(hostId).getTrace(traceOffs);
    }


    public PagingData pageTraces(int hostId, int offset, int limit, TraceListFilterExpression filter) {

        return storeManager.getHost(hostId).pageTraces(offset, limit, filter);
    }


    public List<MethodRankInfo> traceMethodRank(int hostId, long traceOffs, String orderBy, String orderDesc) {
        TraceRecordStore ctx = storeManager.getHost(hostId).getTraceContext(traceOffs);
        return ctx.methodRank(orderBy, orderDesc);
    }


    public TraceRecordInfo getRecord(int hostId, long traceOffs, long minTime, String path) {

        TraceRecordStore ctx = storeManager.getHost(hostId).getTraceContext(traceOffs);
        return ctx.packTraceRecord(ctx.getTraceRecord(path, minTime), path, null);
    }


    public List<TraceRecordInfo> listRecords(int hostId, long traceOffs, long minTime, String path) {

        // TODO this is propably useless now ...
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


    public TraceRecordSearchResult searchRecords(int hostId, long traceOffs, long minTime, String path,
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

}
