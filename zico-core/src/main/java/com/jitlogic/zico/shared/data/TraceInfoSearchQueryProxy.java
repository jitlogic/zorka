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
package com.jitlogic.zico.shared.data;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.jitlogic.zico.core.model.TraceInfoSearchQuery;

@ProxyFor(TraceInfoSearchQuery.class)
public interface TraceInfoSearchQueryProxy extends ValueProxy {

    public static final int ORDER_DESC  = 0x0001;
    public static final int DEEP_SEARCH = 0x0002;
    public static final int ERRORS_ONLY = 0x0004;

    int getSeq();

    void setSeq(int seq);

    String getHostName();

    void setHostName(String hostName);

    int getFlags();

    void setFlags(int flags);

    int getLimit();

    void setLimit(int limit);

    long getOffset();

    void setOffset(long offset);

    String getTraceName();

    void setTraceName(String name);

    long getMinMethodTime();

    void setMinMethodTime(long minMethodTime);

    String getSearchExpr();

    void setSearchExpr(String expr);
}
