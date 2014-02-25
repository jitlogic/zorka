/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.core.ZicoUtil;
import com.jitlogic.zico.core.eql.EqlException;
import com.jitlogic.zico.core.eql.EqlParseException;
import com.jitlogic.zico.core.model.TraceInfoRecord;
import com.jitlogic.zico.core.TraceRecordStore;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.model.MethodRankInfo;
import com.jitlogic.zico.core.model.TraceInfoSearchQuery;
import com.jitlogic.zico.core.model.TraceRecordSearchQuery;
import com.jitlogic.zico.core.model.TraceInfoSearchResult;
import com.jitlogic.zico.core.model.TraceRecordInfo;
import com.jitlogic.zico.core.model.TraceRecordSearchResult;
import com.jitlogic.zico.core.model.User;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.FullTextTraceRecordMatcher;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class TraceDataGwtService {

    private final static Logger log = LoggerFactory.getLogger(TraceDataGwtService.class);

    private HostStoreManager storeManager;
    private UserManager userManager;

    @Inject
    public TraceDataGwtService(HostStoreManager storeManager, UserManager userManager) {
        this.storeManager = storeManager;
        this.userManager = userManager;
    }


    public TraceInfoSearchResult searchTraces(TraceInfoSearchQuery query) {
        userManager.checkHostAccess(query.getHostName());
        try {
            HostStore host = storeManager.getHost(query.getHostName(), false);
            if (host == null) {
                throw new ZicoRuntimeException("Unknown host: " + query.getHostName());
            }
            return host.search(query);
        } catch (EqlParseException e) {
            log.error("Error while parsing eql query '" + query.getSearchExpr() + "'\n" + e, e);
            throw new ZicoRuntimeException(e.toString() + " [query '" + query.getSearchExpr() + "']", e);
        } catch (EqlException e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException(e.toString() + " [query '" + query.getSearchExpr() + "']", e);
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error while searching '" + query.getSearchExpr() + "': " + e.getMessage(), e);
        }
    }


    public List<MethodRankInfo> traceMethodRank(String hostName, long traceOffs, String orderBy, String orderDesc) {
        userManager.checkHostAccess(hostName);
        try {
            HostStore host = storeManager.getHost(hostName, false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(traceOffs);
                if (info != null && host.getTraceDataStore() != null) {
                    return host.getTraceDataStore().methodRank(info, orderBy, orderDesc);
                }
            }
            return Collections.EMPTY_LIST;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error while calling MethodRank", e);
        }

    }


    public TraceRecordInfo getRecord(String hostName, long traceOffs, long minTime, String path) {
        userManager.checkHostAccess(hostName);
        try {
            HostStore host = storeManager.getHost(hostName, false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(traceOffs);
                if (info != null) {
                    return ZicoUtil.packTraceRecord(host.getSymbolRegistry(),
                            host.getTraceDataStore().getTraceRecord(info, path, minTime), path, null);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error calling getRecord()", e);
        }
    }


    public List<TraceRecordInfo> listRecords(String hostName, long traceOffs, long minTime, String path, boolean recursive) {
        userManager.checkHostAccess(hostName);
        try {
            HostStore host = storeManager.getHost(hostName, false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(traceOffs);
                TraceRecordStore ctx = host.getTraceDataStore();
                if (info != null && ctx != null) {
                    TraceRecord tr = ctx.getTraceRecord(info, path, minTime);

                    List<TraceRecordInfo> lst = new ArrayList<TraceRecordInfo>();

                    if (path != null) {
                        packRecords(host.getSymbolRegistry(), path, ctx, tr, lst, recursive);
                    } else {
                        lst.add(ZicoUtil.packTraceRecord(host.getSymbolRegistry(), tr, "", 250));
                        if (recursive) {
                            packRecords(host.getSymbolRegistry(), "", ctx, tr, lst, recursive);
                        }
                    }
                    return lst;
                }
            }

            return Collections.EMPTY_LIST;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error calling listRecords()", e);
        }
    }


    private void packRecords(SymbolRegistry symbolRegistry, String path, TraceRecordStore ctx, TraceRecord tr,
                             List<TraceRecordInfo> lst, boolean recursive) {
        for (int i = 0; i < tr.numChildren(); i++) {
            TraceRecord child = tr.getChild(i);
            String childPath = path.length() > 0 ? (path + "/" + i) : "" + i;
            lst.add(ZicoUtil.packTraceRecord(symbolRegistry, child, childPath, 250));
            if (recursive && child.numChildren() > 0) {
                packRecords(symbolRegistry, childPath, ctx, child, lst, recursive);
            }
        }
    }


    public TraceRecordSearchResult searchRecords(String hostName, long traceOffs, long minTime, String path,
                                                 TraceRecordSearchQuery expr) {
        userManager.checkHostAccess(hostName);
        try {
            HostStore host = storeManager.getHost(hostName, false);
            if (host != null) {
                TraceInfoRecord info = host.getInfoRecord(traceOffs);
                TraceRecordStore ctx = host.getTraceDataStore();
                if (ctx != null && info != null) {
                    TraceRecord tr = ctx.getTraceRecord(info, path, minTime);
                    TraceRecordSearchResult result = new TraceRecordSearchResult();
                    result.setResult(new ArrayList<TraceRecordInfo>());
                    result.setMinTime(Long.MAX_VALUE);
                    result.setMaxTime(Long.MIN_VALUE);

                    TraceRecordMatcher matcher;
                    String se = expr.getSearchExpr();
                    switch (expr.getType()) {
                        case TraceRecordSearchQuery.TXT_QUERY:
                            if (se != null && se.startsWith("~")) {
                                int rflag = 0 != (expr.getFlags() & TraceRecordSearchQuery.IGNORE_CASE) ? Pattern.CASE_INSENSITIVE : 0;
                                Pattern regex = Pattern.compile(se.substring(1, se.length()), rflag);
                                matcher = new FullTextTraceRecordMatcher(host.getSymbolRegistry(), expr.getFlags(), regex);
                            } else {
                                matcher = new FullTextTraceRecordMatcher(host.getSymbolRegistry(), expr.getFlags(), se);
                            }
                            break;
                        case TraceRecordSearchQuery.EQL_QUERY:
                            matcher = new EqlTraceRecordMatcher(host.getSymbolRegistry(), Parser.expr(se),
                                expr.getFlags(), tr.getTime(), host.getName());
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
            return null;
        } catch (Exception e) {
            log.error("Error searching for traces", e);
            throw new ZicoRuntimeException("Error calling listRecords()", e);
        }
    }

}
