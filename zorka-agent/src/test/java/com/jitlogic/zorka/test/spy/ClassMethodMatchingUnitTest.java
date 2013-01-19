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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.test.spy;

import com.jitlogic.zorka.spy.SpyMatcherSet;
import com.jitlogic.zorka.test.support.ZorkaFixture;
import com.jitlogic.zorka.spy.SpyMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static com.jitlogic.zorka.spy.SpyLib.SM_NOARGS;

public class ClassMethodMatchingUnitTest extends ZorkaFixture {

    @Test
    public void testSimpleClassOnlyMatch() {
        SpyMatcherSet sms = new SpyMatcherSet(new SpyMatcher(SpyMatcher.BY_CLASS_NAME, 0xFF, "com.jitlogic.zorka.spy.**", "*", null));

        Assert.assertTrue(sms.classMatch("com.jitlogic.zorka.spy.unittest.SomeClass"));
        Assert.assertTrue(sms.classMatch("com.jitlogic.zorka.spy.AClass"));
        Assert.assertFalse(sms.classMatch("comXjitlogicXzorkaXspyXAClass"));
    }

    @Test
    public void testClassMatchWithSingleLevelWildcard() {
        SpyMatcherSet sms = new SpyMatcherSet(new SpyMatcher(SpyMatcher.BY_CLASS_NAME, 0xFF, "com.jitlogic.zorka.spy.*", "*", null));
        Assert.assertFalse(sms.classMatch("com.jitlogic.zorka.spy.unittest.SomeClass"));
        Assert.assertTrue(sms.classMatch("com.jitlogic.zorka.spy.AClass"));
    }

    @Test
    public void testClassMethodMatch() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod("test.SomeClass", "get*"));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "getVal", "()I", null));
        Assert.assertFalse(sms.methodMatch("test.SomeClass", null, null, 1, "setVal", "(I)V", null));
    }

    @Test
    public void testClassMethodStrictMatch() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod("test.SomeClass", "get"));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "get", "()I", null));
        Assert.assertFalse(sms.methodMatch("test.SomeClass", null, null, 1, "getVal", "()I", null));
    }

    @Test
    public void testClassMatchSignatureWithoutTypes() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0, "test.*", "get", null));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "get", "()V", null));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "get", "(II)V", null));
        Assert.assertFalse(sms.methodMatch("test.SomeClass", null, null, 1, "get", "malformed", null));
    }

    @Test
    public void testClassMatchSignatureWithReturnVoidType() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xff, "test.*", "someMethod", "void"));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "someMethod", "()V", null));
        Assert.assertFalse(sms.methodMatch("test.SomeClass", null, null, 1, "someMethod", "()Void", null));
        Assert.assertFalse(sms.methodMatch("test.SomeClass", null, null, 1, "someMethod", "()I", null));
    }

    @Test
    public void testClassMatchSignatureReturnClassType() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xff, "test.*", "get", "java.lang.String"));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "get", "()Ljava/lang/String;", null));
    }

    @Test
    public void testClassMatchSignatureWithSimpleReturnAndArgumentType() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xff, "test.*", "frobnicate", "int", "int"));
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate", "int", "int");
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(I)I", null));
        Assert.assertFalse(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(J)I", null));
    }


    @Test
    public void testClassMatchSignatureWithStringType() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xff, "test.*", "frobnicate", "String", "String"));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(Ljava/lang/String;)Ljava/lang/String;", null));
        Assert.assertFalse(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(J)I", null));
    }

    @Test
    public void testClassMatchWithVariousArgs() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xff, "test.*", "frobnicate", "String", "int", "com.jitlogic.zorka.spy.CallInfo"));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "frobnicate", "(ILcom/jitlogic/zorka/spy/CallInfo;)Ljava/lang/String;", null));
    }

    @Test
    public void testClassMatchWithNoArgsMarker() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xFF, "test.*", "frobnicate", null, SM_NOARGS));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "()V", null));
        Assert.assertFalse(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(I)V", null));
    }

    @Test
    public void testMatchWithMoreAttributesAndNoArgsFlag() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xff, "test.*", "frobnicate", null, "int", SM_NOARGS));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(I)V", null));
        Assert.assertFalse(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(II)V", null));
    }

    @Test
    public void testMatchWithJustSomeAttributes() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod(0xFF, "test.*", "frobnicate", null, "int"));
        Assert.assertTrue(sms.methodMatch("test.SomeClass", null, null, 1, "frobnicate", "(I)V", null));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "frobnicate", "(II)V", null));
    }

    @Test
    public void testMatchOnlyNames() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byMethod("test.someClass", "trivialMethod"));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "trivialMethod", "()V", null));
        Assert.assertTrue(sms.methodMatch("test.someClass", null, null, 1, "trivialMethod", "(II)V", null));
    }

    @Test
    public void testMatchAnnotationBits() {
        SpyMatcherSet sms = new SpyMatcherSet(spy.byClassAnnotation("some.Annotation"));
        Assert.assertTrue(sms.methodMatch(null, Arrays.asList("Lsome.Annotation;"), null, 1, "trivialMethod", "()V", null));
    }


}
