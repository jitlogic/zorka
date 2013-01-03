/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.test.support.ZorkaFixture;
import com.jitlogic.zorka.spy.SpyMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static com.jitlogic.zorka.spy.SpyLib.SM_NOARGS;

public class ClassMethodMatchingUnitTest extends ZorkaFixture {

    @Test
    public void testSimpleClassOnlyMatch() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "com.jitlogic.zorka.spy.**", "*", null);

        Assert.assertTrue(cm.matches(Arrays.asList("com.jitlogic.zorka.spy.unittest.SomeClass")));
        Assert.assertTrue(cm.matches(Arrays.asList("com.jitlogic.zorka.spy.AClass")));
        Assert.assertFalse(cm.matches(Arrays.asList("comXjitlogicXzorkaXspyXAClass")));
    }

    @Test
    public void testClassMatchWithSingleLevelWildcard() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "com.jitlogic.zorka.spy.*", "*", null);

        Assert.assertFalse(cm.matches(Arrays.asList("com.jitlogic.zorka.spy.unittest.SomeClass")));
        Assert.assertTrue(cm.matches(Arrays.asList("com.jitlogic.zorka.spy.AClass")));

    }

    @Test
    public void testClassMethodMatch() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "get*", null);

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "getVal"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "setVal"));
    }

    @Test
    public void testClassMethodStrictMatch() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "get", null);

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "get"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.someClass"), "getValAndSome"));
    }

    @Test
    public void testClassMatchSignatureWithoutTypes() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "get", null);

        Assert.assertTrue(cm.matches(Arrays.asList("test.someClass"), "get", "()V"));
        Assert.assertTrue(cm.matches(Arrays.asList("test.someClass"), "get", "(II)V"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.someClass"), "get", "malformed"));
    }

    @Test
    public void testClassMatchSignatureWithReturnVoidType() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "someMethod", "void");
        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "someMethod", "()V"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "someMethod", "()Void"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "someMethod", "()I"));
    }

    @Test
    public void testClassMatchSignatureReturnClassType() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "get", "java.lang.String");

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "get", "()Ljava/lang/String;"));
    }

    @Test
    public void testClassMatchSignatureWithSimpleReturnAndArgumentType() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate", "int", "int");

        Assert.assertTrue(cm.matches(Arrays.asList("test.someClass"), "frobnicate", "(I)I"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(J)I"));
    }


    @Test
    public void testClassMatchSignatureWithStringType() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate", "String", "String");

        Assert.assertTrue(cm.matches(Arrays.asList("test.someClass"), "frobnicate", "(Ljava/lang/String;)Ljava/lang/String;"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(J)I"));
    }

    @Test
    public void testClassMatchWithVariousArgs() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate",
                "String", "int", "com.jitlogic.zorka.spy.CallInfo");

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate",
                "(ILcom/jitlogic/zorka/spy/CallInfo;)Ljava/lang/String;"));
    }

    @Test
    public void testClassMatchWithNoArgsMarker() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate", null, SM_NOARGS);

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "()V"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(I)V"));
    }

    @Test
    public void testMatchWithMoreAttributesAndNoArgsFlag() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate", null, "int", SM_NOARGS);

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(I)V"));
        Assert.assertFalse(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(II)V"));
    }

    @Test
    public void testMatchWithJustSomeAttributes() {
        SpyMatcher cm = new SpyMatcher(0, 0xFF, "test.*", "frobnicate", null, "int");

        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(I)V"));
        Assert.assertTrue(cm.matches(Arrays.asList("test.SomeClass"), "frobnicate", "(II)V"));
    }

    @Test
    public void testMatchOnlyNames() {
        SpyMatcher cm = new SpyMatcher(0, SpyMatcher.DEFAULT_FILTER, "test.someClass", "trivialMethod", null);

        Assert.assertTrue(cm.matches(Arrays.asList("test.someClass"), "trivialMethod", "()V", 1));
        Assert.assertTrue(cm.matches(Arrays.asList("test.someClass"), "trivialMethod", "(I)I", 1));
    }

    @Test
    public void testMatchAnnotationBits() {
        SpyMatcher m = spy.byClassAnnotation("some.Annotation");

        Assert.assertEquals(true, m.hasClassAnnotation());
        Assert.assertTrue(m.matches(Arrays.asList("Lsome.Annotation;"), "trivialMethod", "()V", 1));
    }


}
