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
package com.jitlogic.zico.test;

import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.search.EqlTraceRecordMatcher;
import com.jitlogic.zico.core.search.FullTextTraceRecordMatcher;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.jitlogic.zico.core.model.TraceDetailSearchExpression.IGNORE_CASE;
import static com.jitlogic.zico.core.model.TraceDetailSearchExpression.SEARCH_ATTRS;
import static com.jitlogic.zico.core.model.TraceDetailSearchExpression.SEARCH_CLASSES;
import static com.jitlogic.zico.core.model.TraceDetailSearchExpression.SEARCH_METHODS;
import static com.jitlogic.zico.core.model.TraceDetailSearchExpression.SEARCH_SIGNATURE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TraceRecordMatchingUnitTest extends ZicoFixture {

    int flags;
    TraceRecord tr1;
    TraceRecord tr2;
    TraceRecord tr3;


    private TraceRecord tr(String className, String methodName, String methodSignature) {
        TraceRecord tr = new TraceRecord(null);
        tr.setClassId(symbolRegistry.symbolId(className));
        tr.setMethodId(symbolRegistry.symbolId(methodName));
        tr.setSignatureId(symbolRegistry.symbolId(methodSignature));
        return tr;
    }

    private boolean eqlm(String expr, TraceRecord tr) {
        return new EqlTraceRecordMatcher(symbolRegistry, Parser.expr(expr), 0, 1000000000).match(tr);
    }

    @Before
    public void mkRecords() {
        tr1 = tr("com.mysql.jdbc.PreparedStatement", "execute", "()V");
        tr1.setAttr(symbolRegistry.symbolId("SQL"), "select 1");
        tr2 = tr("com.mysql.jdbc.Statement", "executeUpdate", "()V");
        tr3 = tr("com.mysql.jdbc.PreparedStatement", "execute", "(Ljava/lang/String;)V");
        flags = SEARCH_METHODS | SEARCH_CLASSES | SEARCH_ATTRS;
    }


    @Test
    public void testFullTextMatching() throws Exception {
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, flags, "execute").match(tr1));
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, flags, "execute").match(tr2));
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, flags, "Statement").match(tr1));
        assertFalse(new FullTextTraceRecordMatcher(symbolRegistry, flags, "executeUpdate").match(tr1));
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, SEARCH_SIGNATURE, "execute()").match(tr1));
        assertFalse(new FullTextTraceRecordMatcher(symbolRegistry, SEARCH_SIGNATURE, "execute()").match(tr2));
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, SEARCH_SIGNATURE, "execute(String)").match(tr3));
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, SEARCH_SIGNATURE,
                "void com.mysql.jdbc.PreparedStatement.execute()").match(tr1));
    }


    @Test
    public void testRegexMatching() throws Exception {
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, flags, Pattern.compile("exec.*te")).match(tr1));
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, flags, Pattern.compile("exec.*te")).match(tr2));
    }


    @Test
    public void testCaseSensitivityInFullTextMatching() throws Exception {
        assertTrue(new FullTextTraceRecordMatcher(symbolRegistry, flags | IGNORE_CASE, "ExEcUtE").match(tr1));
        assertFalse(new FullTextTraceRecordMatcher(symbolRegistry, flags, "ExEcUtE").match(tr1));
    }


    @Test
    public void testEqlMatching() throws Exception {
        assertTrue(eqlm("method = 'execute'", tr1));
        assertFalse(eqlm("method = 'executeUpdate'", tr1));
        assertTrue(eqlm("class = 'com.mysql.jdbc.PreparedStatement' and method = 'execute'", tr1));

        assertTrue(eqlm("SQL = 'select 1'", tr1));
        assertFalse(eqlm("SQL = 'select 1'", tr2));

        assertTrue(eqlm("SQL = null", tr2));
        assertTrue(eqlm("SQL <> null", tr1));
        assertFalse(eqlm("SQL != null", tr2));
    }

}
