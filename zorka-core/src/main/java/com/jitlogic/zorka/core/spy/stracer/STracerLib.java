/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.stracer;

import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyStateShelfSet;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.TracerLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class STracerLib extends TracerLib {

    public static final Logger log = LoggerFactory.getLogger(STracerLib.class);

    public STracerLib(SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry, Tracer tracer, ZorkaConfig config,
                      SpyStateShelfSet<DTraceState> shelfSet) {
        super(symbolRegistry, metricsRegistry, tracer, config, shelfSet);
    }


    public ZorkaAsyncThread<SymbolicRecord> toCbor(Map<String, String> config) {
        return new STraceHttpOutput(this.config, config, symbolRegistry);
    }


    public void setTraceSpyMethods(boolean tsm) {

    }

    public boolean isTraceSpyMethods() {
        return false;
    }

}
