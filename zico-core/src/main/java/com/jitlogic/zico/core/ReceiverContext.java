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
package com.jitlogic.zico.core;

import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zorka.common.tracedata.MetadataChecker;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReceiverContext implements MetadataChecker, ZicoDataProcessor {

    private final static Logger log = LoggerFactory.getLogger(ReceiverContext.class);

    private Map<Integer, Integer> sidMap = new HashMap<Integer,Integer>();

    private boolean dirtySidMap = false;

    private HostStore hostStore;

    private Set<Object> visitedObjects = new HashSet<Object>();


    public ReceiverContext(HostStore hostStore) {
        this.hostStore = hostStore;
    }


    @Override
    public synchronized void process(Object obj) throws IOException {

        if (hostStore.hasFlag(HostProxy.DELETED)) {
            log.info("Resetting connection for " + hostStore.getName() + " due to dirty SID map.");
            throw new ZicoException(ZicoPacket.ZICO_EOD,
                    "Host has been deleted. Connection needs to be reset. Try again.");
        }

        if (hostStore.hasFlag(HostProxy.DISABLED)) {
            // Host store is disabled. Ignore incoming packets.
            return;
        }

        if (dirtySidMap) {
            log.info("Resetting connection for " + hostStore.getName() + " due to dirty SID map.");
            throw new ZicoException(ZicoPacket.ZICO_EOD,
                "Host was disabled, then enabled and SID map is dirty. Resetting connection.");
        }

        try {
            if (obj instanceof Symbol) {
                processSymbol((Symbol) obj);
            } else if (obj instanceof TraceRecord) {
                processTraceRecord((TraceRecord) obj);
            } else {
                if (obj != null) {
                    log.warn("Unsupported object type:" + obj.getClass());
                } else {
                    log.warn("Attempted processing NULL object (?)");
                }
            }
        } catch (Exception e) {
            log.error("Error processing trace record: ", e);
        }
    }


    public void commit() {
        hostStore.commit();
    }


    private void processSymbol(Symbol sym) {
        if (hostStore.getSymbolRegistry() != null) {
            int newid = hostStore.getSymbolRegistry().symbolId(sym.getName());
            sidMap.put(sym.getId(), newid);
        } else {
            dirtySidMap = true;
        }
    }


    private void processTraceRecord(TraceRecord rec) throws IOException {
        if (!hostStore.hasFlag(HostProxy.DISABLED)) {
            rec.traverse(this);
            visitedObjects.clear();
            hostStore.processTraceRecord(rec);
        } else {
            log.debug("Dropping trace for inactive host: " + hostStore.getName());
        }
    }



    @Override
    public int checkSymbol(int symbolId, Object owner) throws IOException {
        if (owner instanceof TraceMarker) {
            if (visitedObjects.contains(owner)) {
                return symbolId;
            } else {
                visitedObjects.add(owner);
            }
        }
        return sidMap.containsKey(symbolId) ? sidMap.get(symbolId) : 0;
    }


    @Override
    public void checkMetric(int metricId) throws IOException {
    }


    public HostStore getHostStore() {
        return hostStore;
    }
}
