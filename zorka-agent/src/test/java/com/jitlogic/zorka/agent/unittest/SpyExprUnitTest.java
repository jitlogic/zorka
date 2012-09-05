/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.agent.unittest;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import com.jitlogic.zorka.spy.SpyExprMacro;
import com.jitlogic.zorka.spy.SpyExpression;

public class SpyExprUnitTest {
	
	
	private List<Object> lst(Object...objs) {
		return Arrays.asList(objs);
	}
	
	
	private SpyExprMacro mac(int num, String...segs) {
		return new SpyExprMacro(num, Arrays.asList(segs));
	}
	
	private String format(String expr, Object...args) {
		return SpyExpression.parse(expr).format(args);
	}
	
	
	@Test
	public void testStringWithNoMacros() {
		SpyExpression expr = SpyExpression.parse("someMethod");
		assertEquals("segments", lst("someMethod"), expr.getSegs());
	}
	
	@Test
	public void testStringMacroOnly() { 
		SpyExpression ex = SpyExpression.parse("{1}");
		assertEquals("segments", lst(mac(1)), ex.getSegs());
		assertEquals("argmap", lst(1), ex.getArgMap());
	}
	
	@Test
	public void testStringWithEmbeddedMacro() {
		SpyExpression ex = SpyExpression.parse("some_{2}.stuff");
		assertEquals("segments", lst("some_", mac(2), ".stuff"), ex.getSegs());
		assertEquals("argmap", lst(2), ex.getArgMap());
	}
	
	@Test
	public void testStringMacroWithSegs() {
		SpyExpression ex = SpyExpression.parse("{1.someAttr.otherAttr}");
		assertEquals("segments", lst(mac(1, "someAttr", "otherAttr")), ex.getSegs());
		assertEquals("argmap", lst(1), ex.getArgMap());
	}	
	
	@Test
	public void testFormatSimpleArg() {
		assertEquals("oja!", format("{1}", "oja!"));
	}
	
	@Test
	public void testFormatWithArgEmbeddedInString() {
		assertEquals("some.test.stuff", format("some.{1}.stuff", "test"));
	}
	
	@Test
	public void testFormatWithAttrGetter() {
		// TODO fix assertEquals("java.lang.String", format("{1.class.name}", "oja!"));
	}
}
