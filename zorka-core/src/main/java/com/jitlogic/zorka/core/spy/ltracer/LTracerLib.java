/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.ltracer;

import java.io.IOException;
import java.util.Map;

import com.jitlogic.zorka.common.http.HttpClient;
import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.integ.zabbix.ZabbixTraceOutput;
import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.core.spy.output.LTraceHttpOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracer library contains functions for configuring and using tracer.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LTracerLib extends TracerLib {

    public static final Logger log = LoggerFactory.getLogger(LTracerLib.class);

    private MethodCallStatistics stats;

    /**
     * Creates tracer library object.
     *
     * @param tracer reference to spy instance
     */
    public LTracerLib(SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry, Tracer tracer, ZorkaConfig config, MethodCallStatistics stats) {
        super(symbolRegistry, metricsRegistry, tracer, config);
        this.stats = stats;
    }

    @Override
    public ZorkaAsyncThread<SymbolicRecord> toCbor(Map<String, String> config) {
        return new LTraceHttpOutput(this.config, config, symbolRegistry, HttpClient.fromMap(config, stats));
    }

    /**
     * Creates trace network sender. It will receive traces and send them to remote Zabbix Server.
     *
     * @param addr     collector host name or IP address
     * @param port     collector port
     * @param hostname agent name - this will be presented in collector console;
     * @param qlen
     * @param packetSize
     * @param interval
     * @return
     * @throws IOException
     */
    public ZorkaAsyncThread<SymbolicRecord> toZabbix(String addr, int port, String hostname, int qlen, 
                                                   long packetSize, int retries, long retryTime, long retryTimeExp, 
                                                   int timeout, int interval) throws IOException {
        ZabbixTraceOutput output = new ZabbixTraceOutput(symbolRegistry, metricsRegistry, addr, port, hostname, qlen, packetSize,
        		retries, retryTime, retryTimeExp, timeout, interval);
        output.start();
        return output;
    }

}
