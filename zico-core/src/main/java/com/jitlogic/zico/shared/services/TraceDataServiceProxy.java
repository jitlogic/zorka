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
package com.jitlogic.zico.shared.services;


import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import com.jitlogic.zico.core.inject.ZicoServiceLocator;
import com.jitlogic.zico.core.services.TraceDataGwtService;
import com.jitlogic.zico.shared.data.MethodRankInfoProxy;
import com.jitlogic.zico.shared.data.TraceInfoSearchQueryProxy;
import com.jitlogic.zico.shared.data.TraceRecordSearchQueryProxy;
import com.jitlogic.zico.shared.data.TraceInfoSearchResultProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;
import com.jitlogic.zico.shared.data.TraceRecordSearchResultProxy;

import java.util.List;

@Service(value = TraceDataGwtService.class, locator = ZicoServiceLocator.class)
public interface TraceDataServiceProxy extends RequestContext {

    public static final int DESC_ORDER = 1;
    public static final int RECURSIVE  = 2;

    Request<TraceInfoSearchResultProxy> searchTraces(TraceInfoSearchQueryProxy query);

    Request<List<MethodRankInfoProxy>> traceMethodRank(String hostName, long traceOffs, String orderBy, String orderDesc);

    Request<TraceRecordProxy> getRecord(String hostName, long traceOffs, long minTime, String path);

    Request<List<TraceRecordProxy>> listRecords(String hostName, long traceOffs, long minTime, String path, boolean recursive);

    Request<TraceRecordSearchResultProxy> searchRecords(String hostName, long traceOffs, long minTime, String path,
                                                  TraceRecordSearchQueryProxy expr);

}
