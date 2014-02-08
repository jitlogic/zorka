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
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.eql.EqlException;
import com.jitlogic.zico.core.eql.EqlExprEvaluator;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.eql.ast.EqlExpr;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EqlEvaluationUnitTest {


    public static class TestEvaluator extends EqlExprEvaluator {

        private Map<String, Object> symbols = ZorkaUtil.map(
                "a", 1,
                "b", 2
        );

        @Override
        protected Object resolve(String name) {
            if (!symbols.containsKey(name)) {
                throw new EqlException("Cannot resolve symbol: '" + name + "'");
            }
            return symbols.get(name);
        }
    }


    private Object eval(String expr) {
        EqlExpr rslt = new Parser().expr(expr);
        assertNotNull("Result should not be null.", rslt);
        return rslt.accept(new TestEvaluator());
    }


    @Test
    public void testComparisonExprs() {
        assertEquals(true, eval("1 = 1"));
        assertEquals(false, eval("1 <> 1"));
        assertEquals(true, eval("1 <> 2"));
        assertEquals(false, eval("1 = 2"));
        assertEquals(true, eval("1 != 2"));

        assertEquals(true, eval("a = 1"));
        assertEquals(false, eval("b = 1"));

        assertEquals(true, eval("'abc' = 'abc'"));
        assertEquals(false, eval("'abc' = 'def'"));

        assertEquals(true, eval("42L = 42L"));

        assertEquals(true, eval("42 = 42L"));

        assertEquals(true, eval("1 < 2"));
        assertEquals(false, eval("1 > 2"));

        assertEquals(true, eval("1 <= 2"));
        assertEquals(true, eval("1 <= 1"));

        assertEquals(false, eval("1 >= 2"));
        assertEquals(true, eval("1 >= 1"));

    }


    @Test
    public void testRegexMatchExpressions() {
        assertEquals(true, eval("'abcde' ~= 'a.*e'"));
        assertEquals(false, eval("'abcde' ~= 'a.*f'"));
        assertEquals(true, eval("'abcde' ~= 'bcd'"));
        assertEquals(false, eval("'abcde' ~= '^bcd$'"));
        assertEquals(true, eval("'abcde' ~= '^abc'"));
    }


    @Test
    public void testArithmeticExpressions() {
        assertEquals(4, eval("2 + 2"));
        assertEquals(4, eval("b + 2"));
        assertEquals(4L, eval("b + 2L"));
        assertEquals(16, eval("4 * 4"));
        assertEquals(5, eval("25 / 5"));
        assertEquals(25, eval("3 * 3 + 4 * 4"));
        assertEquals(1, eval("25 % 3"));
        assertEquals(65000000000L, eval("1m+5s"));

        assertEquals("abcd", eval("'ab' + 'cd'"));
    }


    @Test
    public void testBitwiseExpressions() {
        assertEquals(3, eval("1 | 2"));
        assertEquals(1, eval("1 & 3"));
        assertEquals(2, eval("1 ^ 3"));
    }


    @Test
    public void testLogicalExpressions() {
        assertEquals(true, eval("2 = 2 and 4 = 4"));
        assertEquals(true, eval("2 = 2 && 4 = 4"));
        assertEquals(true, eval("4 = 3 or 2 = 2"));
        assertEquals(true, eval("4 = 3 || 2 = 2"));
        assertEquals(false, eval("4 = 4 and 2 = 3"));
    }


    @Test
    public void testEvalUnaryExprs() {
        assertEquals(false, eval("not true"));
        assertEquals(true, eval("! 1 = 2"));
        assertEquals(0, eval("~ -1"));
    }
}
