/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.viewer;


import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.objectweb.asm.Type;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewerTraceRecord extends TraceRecord {

    /**
     * Print short class name
     */
    public static final int PS_SHORT_CLASS = 0x01;

    /**
     * Print result type
     */
    public static final int PS_RESULT_TYPE = 0x02;

    /**
     * Print short argument types
     */
    public static final int PS_SHORT_ARGS = 0x04;

    /**
     * Omits arguments overall in pretty pring
     */
    public static final int PS_NO_ARGS = 0x08;


    private SymbolRegistry symbols;
    private int recs, level;
    private double timePct = 0.0;
    private boolean expanded = true;


    public ViewerTraceRecord(SymbolRegistry symbols) {
        super(null);
        this.symbols = symbols;
    }


    public boolean hasError() {
        return hasFlag(ViewerTraceRecord.EXCEPTION_PASS) || this.getException() != null;
    }


    public String getTraceLabel() {
        StringBuilder sb = new StringBuilder();
        TraceMarker m = getMarker();
        sb.append(m != null ? sym(m.getTraceId()) : "?");

        if (getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : getAttrs().entrySet()) {
                sb.append('|');
                sb.append(e.getValue());
            }
        }
        return sb.toString();
    }


    public void toggleExpanded() {
        expanded = !expanded;
    }

    public String getClassName() {
        return sym(getClassId());
    }

    public String getMethodName() {
        return sym(getClassId());
    }


    public String sym(long id) {
        return symbols.symbolName((int) id);
    }

    public String sym(int id) {
        return symbols.symbolName(id);
    }


    public int getRecs() {
        return recs;
    }


    public void fixup() {
        fixup(getTime(), 0);
    }


    /**
     * Calculates method execution time percentage and recursion level
     * for this record and all children recursively
     *
     * @param total total trace execution time
     * @param level recursion level of parent method
     */
    public void fixup(long total, int level) {
        timePct = 100.0 * this.getTime() / total;
        this.level = level;
        this.recs = 1;

        if (getChildren() != null) {
            for (TraceRecord child : getChildren()) {
                ((ViewerTraceRecord) child).fixup(total, level + 1);
                recs += ((ViewerTraceRecord) child).getRecs();
            }
        }
    }


    public String prettyClock() {
        return new SimpleDateFormat("hh:mm:ss.SSS").format(getClock());
    }


    /**
     * Returns human readable method description (with default flags)
     *
     * @return method description string
     */
    public String prettyPrint() {
        return prettyPrint(PS_RESULT_TYPE | PS_SHORT_ARGS);
    }


    /**
     * Returns human readable method description (configurable with supplied flags)
     *
     * @param style style flags (see PS_* constants)
     * @return method description string
     */
    public String prettyPrint(int style) {
        StringBuffer sb = new StringBuffer(128);

        String signature = sym(getSignatureId());

        // Print return type
        if (0 != (style & PS_RESULT_TYPE)) {
            Type retType = Type.getReturnType(signature);
            if (0 != (style & PS_SHORT_ARGS)) {
                sb.append(ViewerUtil.shortClassName(retType.getClassName()));
            } else {
                sb.append(retType.getClassName());
            }
            sb.append(" ");
        }

        // Print class name
        if (0 != (style & PS_SHORT_CLASS)) {
            sb.append(ViewerUtil.shortClassName(sym(getClassId())));
        } else {
            sb.append(sym(getClassId()));
        }

        sb.append(".");
        sb.append(sym(getMethodId()));
        sb.append("(");

        // Print arguments (if needed)
        if (0 == (style & PS_NO_ARGS)) {
            Type[] types = Type.getArgumentTypes(signature);
            for (int i = 0; i < types.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (0 != (style & PS_SHORT_ARGS)) {
                    sb.append(ViewerUtil.shortClassName(types[i].getClassName()));
                } else {
                    sb.append(types[i].getClassName());
                }
            }
        }

        sb.append(")");

        return sb.toString();
    }


    /**
     * Creates flat-list representation of trace record tree.
     *
     * @param result list object to be populated
     */
    public void scanRecords(List<ViewerTraceRecord> result, ViewerRecordFilter filter) {
        if (filter == null || filter.matches(this)) {
            result.add(this);
        }

        if (getChildren() != null && expanded && (filter == null || filter.recurse(this))) {
            for (TraceRecord child : getChildren()) {
                ((ViewerTraceRecord) child).scanRecords(result, filter);
            }
        }
    }


    public int getLevel() {
        return level;
    }


    public double getTimePct() {
        return timePct;
    }


    public boolean isExpanded() {
        return expanded;
    }


    public String getTraceName() {
        return getMarker() != null ? sym(getMarker().getTraceId()) : null;
    }

    public Map<String, Object> listAttrs() {
        Map<String, Object> ret = new HashMap<String, Object>();

        if (getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : getAttrs().entrySet()) {
                ret.put(sym(e.getKey()), e.getValue());
            }
        }

        return ret;
    }

}
