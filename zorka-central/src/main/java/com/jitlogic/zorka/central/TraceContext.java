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
package com.jitlogic.zorka.central;


import com.jitlogic.zorka.central.data.SymbolicExceptionInfo;
import com.jitlogic.zorka.central.data.TraceDetailFilterExpression;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.jitlogic.zorka.central.rds.RDSStore;
import com.jitlogic.zorka.common.tracedata.*;
import org.fressian.FressianReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TraceContext {

    private HostStore hostStore;
    private TraceInfo traceInfo;

    private TraceCache cache;
    private SymbolRegistry symbolRegistry;


    public TraceContext(HostStore hostStore, TraceInfo traceInfo, TraceCache cache, SymbolRegistry symbolRegistry) {
        this.hostStore = hostStore;
        this.traceInfo = traceInfo;

        this.cache = cache;
        this.symbolRegistry = symbolRegistry;
    }


    private final static Pattern RE_SLASH = Pattern.compile("/");


    public TraceRecord getTraceRecord(String path) {
        try {
            TraceRecord tr = fetchRecord();
            if (path != null && path.trim().length() > 0) {
                for (String p : RE_SLASH.split(path.trim())) {
                    Integer idx = Integer.parseInt(p);
                    if (idx >= 0 && idx < tr.numChildren()) {
                        tr = tr.getChild(idx);
                    } else {
                        throw new RuntimeException("Child record of path " + path + " not found.");
                    }
                }
            }

            return tr;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving trace record.", e);
        }
    }


    private TraceRecord fetchRecord() throws IOException {
        TraceDetailFilterExpression filter = new TraceDetailFilterExpression();
        filter.setHostId(hostStore.getHostInfo().getId());
        filter.setTraceOffs(traceInfo.getDataOffs());

        TraceRecord tr = cache.get(filter);

        if (tr == null) {
            RDSStore rds = hostStore.getRds();
            byte[] blob = rds.read(traceInfo.getDataOffs(), traceInfo.getDataLen());
            ByteArrayInputStream is = new ByteArrayInputStream(blob);
            FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            tr = (TraceRecord) reader.readObject();
            cache.put(filter, tr);
        }

        return tr;
    }


    public TraceRecordInfo packTraceRecord(TraceRecord tr, String path) {
        TraceRecordInfo info = new TraceRecordInfo();

        info.setCalls(tr.getCalls());
        info.setErrors(tr.getErrors());
        info.setTime(tr.getTime());
        info.setFlags(tr.getFlags());
        info.setMethod(CentralUtil.prettyPrint(tr, symbolRegistry));
        info.setChildren(tr.numChildren());
        info.setPath(path);

        if (tr.getAttrs() != null) {
            Map<String, String> nattr = new HashMap<String, String>();
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                String s = "" + e.getValue();
                if (s.length() > 250) {
                    s = s.substring(0, 250) + "...";
                }
                nattr.put(symbolRegistry.symbolName(e.getKey()), s);
            }
            info.setAttributes(nattr);
        }

        SymbolicException sex = tr.findException();
        if (sex != null) {
            SymbolicExceptionInfo sei = new SymbolicExceptionInfo();
            sei.setExClass(symbolRegistry.symbolName(sex.getClassId()));
            sei.setMessage(sex.getMessage());
            List<String> stack = new ArrayList<String>(sex.getStackTrace().length);
            for (SymbolicStackElement sel : sex.getStackTrace()) {
                stack.add("  at " + symbolRegistry.symbolName(sel.getClassId())
                        + "." + symbolRegistry.symbolName(sel.getMethodId())
                        + " [" + symbolRegistry.symbolName(sel.getFileId())
                        + ":" + sel.getLineNum() + "]");
            }
            sei.setStackTrace(stack);
            info.setExceptionInfo(sei);
        }

        return info;
    }

}
