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
		assertEquals("java.lang.String", format("{1.class.name}", "oja!"));
	}
}
