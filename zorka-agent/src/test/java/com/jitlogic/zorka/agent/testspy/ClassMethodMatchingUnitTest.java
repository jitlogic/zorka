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
package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.spy.SpyMatcher;
import org.junit.Assert;
import org.junit.Test;

import static com.jitlogic.zorka.spy.SpyConst.*;

public class ClassMethodMatchingUnitTest {

    @Test
    public void testSimpleClassOnlyMatch() {
        SpyMatcher cm = new SpyMatcher("com.jitlogic.zorka.spy.**", "*", null, 0xFF);

        Assert.assertTrue(cm.matches("com.jitlogic.zorka.spy.unittest.SomeClass"));
        Assert.assertTrue(cm.matches("com.jitlogic.zorka.spy.AClass"));
        Assert.assertFalse(cm.matches("comXjitlogicXzorkaXspyXAClass"));
    }

    @Test
    public void testClassMatchWithSingleLevelWildcard() {
        SpyMatcher cm = new SpyMatcher("com.jitlogic.zorka.spy.*", "*", null, 0xFF);

        Assert.assertFalse(cm.matches("com.jitlogic.zorka.spy.unittest.SomeClass"));
        Assert.assertTrue(cm.matches("com.jitlogic.zorka.spy.AClass"));

    }

    @Test
    public void testClassMethodMatch() {
        SpyMatcher cm = new SpyMatcher("test.*", "get*", null, 0xFF);

        Assert.assertTrue(cm.matches("test.SomeClass", "getVal"));
        Assert.assertFalse(cm.matches("test.SomeClass", "setVal"));
    }

    @Test
    public void testClassMethodStrictMatch() {
        SpyMatcher cm = new SpyMatcher("test.*", "get", null, 0xFF);

        Assert.assertTrue(cm.matches("test.SomeClass", "get"));
        Assert.assertFalse(cm.matches("test.someClass", "getValAndSome"));
    }

    @Test
    public void testClassMatchSignatureWithoutTypes() {
        SpyMatcher cm = new SpyMatcher("test.*", "get", null, 0xFF);

        Assert.assertTrue(cm.matches("test.someClass", "get", "()V"));
        Assert.assertTrue(cm.matches("test.someClass", "get", "(II)V"));
        Assert.assertFalse(cm.matches("test.someClass", "get", "malformed"));
    }

    @Test
    public void testClassMatchSignatureWithReturnVoidType() {
        SpyMatcher cm = new SpyMatcher("test.*", "someMethod", "void", 0xFF);
        Assert.assertTrue(cm.matches("test.SomeClass", "someMethod", "()V"));
        Assert.assertFalse(cm.matches("test.SomeClass", "someMethod", "()Void"));
        Assert.assertFalse(cm.matches("test.SomeClass", "someMethod", "()I"));
    }

    @Test
    public void testClassMatchSignatureReturnClassType() {
        SpyMatcher cm = new SpyMatcher("test.*", "get", "java.lang.String", 0xFF);

        Assert.assertTrue(cm.matches("test.SomeClass", "get", "()Ljava/lang/String;"));
    }

    @Test
    public void testClassMatchSignatureWithSimpleReturnAndArgumentType() {
        SpyMatcher cm = new SpyMatcher("test.*", "frobnicate", "int", 0xFF, "int");

        Assert.assertTrue(cm.matches("test.someClass", "frobnicate", "(I)I"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(J)I"));
    }


    @Test
    public void testClassMatchSignatureWithStringType() {
        SpyMatcher cm = new SpyMatcher("test.*", "frobnicate", "String", 0xFF, "String");

        Assert.assertTrue(cm.matches("test.someClass", "frobnicate", "(Ljava/lang/String;)Ljava/lang/String;"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(J)I"));
    }

    @Test
    public void testClassMatchWithVariousArgs() {
        SpyMatcher cm = new SpyMatcher("test.*", "frobnicate",
                "String", 0xFF, "int", "com.jitlogic.zorka.spy.CallInfo");

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate",
                "(ILcom/jitlogic/zorka/spy/CallInfo;)Ljava/lang/String;"));
    }

    @Test
    public void testClassMatchWithNoArgsMarker() {
        SpyMatcher cm = new SpyMatcher("test.*", "frobnicate", null, 0xFF, SM_NOARGS);

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "()V"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(I)V"));
    }

    @Test
    public void testMatchWithMoreAttributesAndNoArgsFlag() {
        SpyMatcher cm = new SpyMatcher("test.*", "frobnicate", null, 0xFF, "int", SM_NOARGS);

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "(I)V"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(II)V"));
    }

    @Test
    public void testMatchWithJustSomeAttributes() {
        SpyMatcher cm = new SpyMatcher("test.*", "frobnicate", null, 0xFF, "int");

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "(I)V"));
        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "(II)V"));
    }

    @Test
    public void testMatchOnlyNames() {
        SpyMatcher cm = new SpyMatcher("test.someClass", "trivialMethod", null, SpyMatcher.DEFAULT_FILTER);

        Assert.assertTrue(cm.matches("test.someClass", "trivialMethod", "()V", 1));
        Assert.assertTrue(cm.matches("test.someClass", "trivialMethod", "(I)I", 1));
    }

}
