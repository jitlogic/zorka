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
package com.jitlogic.zico.core;

import com.jitlogic.zico.core.locators.TraceTemplateManager;
import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zico.core.model.SymbolicExceptionInfo;
import com.jitlogic.zico.core.model.TraceInfo;
import com.jitlogic.zico.core.model.TraceInfoSearchResult;
import com.jitlogic.zico.core.rds.RDSCleanupListener;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.SymbolicStackElement;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import org.fressian.FressianReader;
import org.fressian.FressianWriter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Responsible for storing trace metadata used when searching through top leve trace list.
 */
public class TraceInfoStore implements RDSCleanupListener, Closeable {

    private final static Logger log = LoggerFactory.getLogger(TraceInfoStore.class);

    public static final int ORDER_DESC  = 1;
    public static final int DEEP_SEARCH = 2;

    private ZicoConfig config;
    private HostStore hostStore;
    private SymbolRegistry symbols;
    private TraceTemplateManager templater;

    private RDSStore rdsIndex;

    private DB db;
    private ConcurrentNavigableMap<Long,TraceInfoRecord> infos;

    private boolean needsCleanup = false;

    public TraceInfoStore(ZicoConfig config, SymbolRegistry symbols, TraceTemplateManager templater, HostStore hostStore) {
        this.config = config;
        this.symbols = symbols;
        this.templater = templater;
        this.hostStore = hostStore;

        openRdsIndex();
        openTraceDb();
    }


    private void openRdsIndex() {
        String rdspath = ZorkaUtil.path(hostStore.getRootPath(), "tindex");

        try {
            long fileSize = config.kiloCfg("rds.file.size", 16 * 1024 * 1024L).intValue();
            long segmentSize = config.kiloCfg("rds.seg.size", 1024 * 1024L);
            rdsIndex = new RDSStore(rdspath,
                    hostStore.getMaxSize(),
                    fileSize,
                    segmentSize);
        } catch (IOException e) {
            log.error("Cannot open RDS store at '" + rdspath + "'", e);
        }
    }


    private void openTraceDb() {
        db = DBMaker.newFileDB(new File(hostStore.getRootPath(), "traces.db")).closeOnJvmShutdown().make();
        infos = db.getTreeMap("INFO_BY_DATA_OFFSET");
    }


    public synchronized void save(TraceRecord rec, long dataOffs, int dataLen) throws IOException {

        if (needsCleanup) {
            // TODO clean up infos table
            // TODO clean up index RDS storage
            db.commit();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);

        List<TraceRecord> tmp = rec.getChildren();
        rec.setChildren(null);
        writer.writeObject(rec);
        rec.setChildren(tmp);

        byte[] indexChunk = os.toByteArray();
        long indexOffs = rdsIndex.write(indexChunk);

        TraceInfoRecord tir = new TraceInfoRecord(rec, dataOffs, dataLen, indexOffs, indexChunk.length);

        infos.put(dataOffs, tir);

        db.commit();
    }


    public synchronized TraceInfoSearchResult search(int flags, long offs, int limit,
                                               TraceRecordMatcher matcher) throws IOException {

        List<TraceInfo> lst = new ArrayList<TraceInfo>(limit);
        TraceInfoSearchResult result = new TraceInfoSearchResult(lst);

        // TODO implement query execution time limit
        // TODO implement deep search

        for (Long key = 0 == (flags & ORDER_DESC) ? infos.higherKey(offs-1) : infos.lowerKey(offs+1);
             key != null;
             key = 0 == (flags & ORDER_DESC) ? infos.higherKey(offs) : infos.lowerKey(offs)) {

            TraceInfoRecord tir = infos.get(infos.get(key));
            TraceRecord idxtr = retrieveFromIndex(tir);
            if (idxtr != null) {
                if (matcher == null || matcher.match(idxtr)) {
                    lst.add(toTraceInfo(tir, idxtr));
                    result.setLastOffs(tir.getDataOffs());
                    if (lst.size() == limit) {
                        result.markFlag(TraceInfoSearchResult.MORE_RESULTS);
                        return result;
                    }
                }
            }
        }

        return result;
    }


    private TraceRecord retrieveFromIndex(TraceInfoRecord tir) throws IOException {
        byte[] blob = rdsIndex.read(tir.getIndexOffs(), tir.getIndexLen());
        // TODO make it general function somewhere in utils - it is used in many places
        if (blob != null) {
            ByteArrayInputStream is = new ByteArrayInputStream(blob);
            FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            return (TraceRecord) reader.readObject();
        }

        return null;
    }


    private TraceInfo toTraceInfo(TraceInfoRecord itr, TraceRecord tr) throws IOException {

        if (tr == null) {
            tr = retrieveFromIndex(itr);
        }

        TraceInfo ti = new TraceInfo();
        ti.setHostId(hostStore.getId());
        ti.setDataOffs(itr.getDataOffs());
        ti.setTraceId(itr.getTraceId());
        ti.setTraceType(symbols.symbolName(itr.getTraceId()));
        ti.setMethodFlags(itr.getRflags());
        ti.setTraceFlags(itr.getTflags());
        ti.setClassId(tr.getClassId());
        ti.setMethodId(tr.getMethodId());
        ti.setSignatureId(tr.getSignatureId());
        ti.setStatus(tr.getException() != null                 // TODO get rid of this field
                || tr.hasFlag(TraceRecord.EXCEPTION_PASS)
                || tr.getMarker().hasFlag(TraceMarker.ERROR_MARK) ? 1 : 0);
        ti.setRecords(itr.getRecords());
        ti.setDataLen(itr.getDataLen());
        ti.setCalls(itr.getCalls());
        ti.setErrors(itr.getErrors());
        ti.setExecutionTime(itr.getDuration());

        if (tr != null && tr.getAttrs() != null) {
            List<KeyValuePair> keyvals = new ArrayList<KeyValuePair>(tr.getAttrs().size());
            for (Map.Entry<Integer,Object> e : tr.getAttrs().entrySet()) {
                keyvals.add(new KeyValuePair("" + e.getKey(), "" + e.getValue()));
            }
            ti.setAttributes(ZicoUtil.sortKeyVals(keyvals));
        } else {
            ti.setAttributes(Collections.EMPTY_LIST);
        }

        if (tr.getException() != null) {
            SymbolicExceptionInfo sei = new SymbolicExceptionInfo();
            SymbolicException sex = (SymbolicException)tr.getException();
            sei.setExClass(symbols.symbolName(sex.getClassId()));
            sei.setMessage(sex.getMessage());
            List<String> lst = new ArrayList<String>();

            for (SymbolicStackElement sel : sex.getStackTrace()) {
                lst.add(symbols.symbolName(sel.getClassId()) + ":" + sel.getLineNum());
                if (lst.size() > 10) {
                    break;
                }
            }

            sei.setStackTrace(lst);

            ti.setExceptionInfo(sei);
        }

        ti.setDescription(templater.templateDescription(ti));

        return ti;
    }


    @Override
    public synchronized void onChunkRemoved(Long start, Long length) {
        needsCleanup = true;
    }


    @Override
    public void onChunkStarted(RDSStore origin, Long start) {
        if (rdsIndex != null) {
            try {
                rdsIndex.rotate();
            } catch (IOException e) {
                log.error("Cannot rotate index for " + hostStore.getName(), e);
            }
        }
    }


    @Override
    public void close() throws IOException {
        rdsIndex.close();
        db.close();
    }
}
