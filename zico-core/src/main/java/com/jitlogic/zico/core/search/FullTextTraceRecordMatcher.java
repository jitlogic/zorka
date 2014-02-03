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
package com.jitlogic.zico.core.search;

import com.jitlogic.zico.core.ZicoUtil;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.SymbolicStackElement;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import java.util.Map;
import java.util.regex.Pattern;

import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.IGNORE_CASE;
import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.SEARCH_ATTRS;
import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.SEARCH_CLASSES;
import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.SEARCH_EX_MSG;
import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.SEARCH_EX_STACK;
import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.SEARCH_METHODS;
import static com.jitlogic.zico.core.model.TraceRecordSearchQuery.SEARCH_SIGNATURE;


public class FullTextTraceRecordMatcher implements TraceRecordMatcher {

    private SymbolRegistry symbolRegistry;

    private int flags;

    private String text;

    private Pattern pattern;


    public FullTextTraceRecordMatcher(SymbolRegistry symbolRegistry, int flags, String text) {
        this.symbolRegistry = symbolRegistry;
        this.flags = flags;

        if (0 != (flags & IGNORE_CASE)) {
            this.text = text != null ? text.toLowerCase() : null;
        } else {
            this.text = text;
        }

    }


    public FullTextTraceRecordMatcher(SymbolRegistry symbolRegistry, int flags, Pattern pattern) {
        this.symbolRegistry = symbolRegistry;
        this.flags = flags;
        this.pattern = pattern;
    }


    @Override
    public boolean hasFlag(int flags) {
        return 0 != (flags & this.flags);
    }


    private boolean matches(int symbol) {
        return matches(symbolRegistry.symbolName(symbol));
    }


    private boolean matches(String s) {

        if (0 != (flags & IGNORE_CASE)) {
            s = s.toLowerCase();
        }

        return s != null &&
                ((text != null && s.contains(text))
                        || (pattern != null && pattern.matcher(s).find()));
    }


    @Override
    public boolean match(TraceRecord tr) {

        if (text == null && pattern == null) {
            return true;
        }

        if ((hasFlag(SEARCH_CLASSES) && matches(tr.getClassId()))
                || (hasFlag(SEARCH_METHODS) && matches(tr.getMethodId()))) {
            return true;
        }

        if (hasFlag(SEARCH_ATTRS) && tr.getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                if (matches(e.getKey())) {
                    return true;
                }
                if (e.getValue() != null && matches(e.getValue().toString())) {
                    return true;
                }
            }
        }

        SymbolicException se = tr.findException();

        if (hasFlag(SEARCH_EX_MSG) && se != null &&
                (matches(se.getMessage())
                        || matches(se.getClassId()))) {
            return true;
        }

        if (hasFlag(SEARCH_EX_STACK) && se != null) {
            for (SymbolicStackElement sse : se.getStackTrace()) {
                if (matches(sse.getClassId())
                        || matches(sse.getMethodId())
                        || matches(sse.getFileId())) {
                    return true;
                }
            }
        }

        if (hasFlag(SEARCH_SIGNATURE)) {
            String signature = ZicoUtil.prettyPrint(tr, symbolRegistry);
            if (matches(signature)) {
                return true;
            }
        }

        return false;
    }

}
