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
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.TraceTypeRegistry;
import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.core.PagingData;
import com.jitlogic.zico.core.TraceInfo;
import com.jitlogic.zico.data.TraceListFilterExpression;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import javax.inject.Inject;

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

}
