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
package com.jitlogic.zico.core.search;


import com.jitlogic.zico.core.ZicoUtil;
import com.jitlogic.zico.core.eql.EqlExprEvaluator;
import com.jitlogic.zico.core.eql.ast.EqlExpr;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;

public class EqlTraceRecordMatcher extends EqlExprEvaluator implements TraceRecordMatcher {

    private SymbolRegistry symbolRegistry;
    private EqlExpr expr;
    private int flags;

    private long totalTime;

    private TraceRecord traceRecord;
    private String signature;


    public EqlTraceRecordMatcher(SymbolRegistry symbolRegistry, EqlExpr expr, int flags, long totalTime) {
        this.symbolRegistry = symbolRegistry;
        this.expr = expr;
        this.flags = flags;
        this.totalTime = totalTime;
    }


    @Override
    public boolean match(TraceRecord tr) {
        this.traceRecord = tr;
        this.signature = null;

        return ZorkaUtil.coerceBool(expr.accept(this));
    }


    @Override
    public boolean hasFlag(int flag) {
        return 0 != (flags & flag);
    }


    @Override
    protected Object resolve(String name) {
        if ("method".equals(name)) {
            return sym(traceRecord.getMethodId());
        } else if ("class".equals(name)) {
            return sym(traceRecord.getClassId());
        } else if ("type".equals(name)) {
            return traceRecord.getMarker() != null ? sym(traceRecord.getMarker().getTraceId()) : null;
        } else if ("time".equals(name)) {
            return traceRecord.getTime();
        } else if ("calls".equals(name)) {
            return traceRecord.getCalls();
        } else if ("errors".equals(name)) {
            return traceRecord.getErrors();
        } else if ("pct".equals(name)) {
            return 100.0 * traceRecord.getTime() / totalTime;
        } else if ("signature".equals(name)) {
            if (signature == null) {
                signature = ZicoUtil.prettyPrint(traceRecord, symbolRegistry);
            }
            return signature;
        } else if (traceRecord.getAttrs() != null) {
            int id = sym(name);
            return id != 0 ? traceRecord.getAttr(id) : null;
        }

        return null;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    private int sym(String symbol) {
        return symbolRegistry.trySymbolId(symbol);
    }

    private String sym(int id) {
        return symbolRegistry.symbolName(id);
    }
}
