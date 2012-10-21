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
package com.jitlogic.zorka.spy.unittest;

import com.jitlogic.zorka.spy.ClassMethodMatcher;
import com.jitlogic.zorka.spy.SpyDefinition;
import org.junit.Assert;
import org.junit.Test;

public class ClassMethodMatchTest {

    @Test
    public void testSimpleClassOnlyMatch() {
        ClassMethodMatcher cm = new ClassMethodMatcher("com.jitlogic.zorka.spy.**", "*", null);

        Assert.assertTrue(cm.matches("com.jitlogic.zorka.spy.unittest.SomeClass"));
        Assert.assertTrue(cm.matches("com.jitlogic.zorka.spy.AClass"));
        Assert.assertFalse(cm.matches("comXjitlogicXzorkaXspyXAClass"));
    }

    @Test
    public void testClassMatchWithSingleLevelWildcard() {
        ClassMethodMatcher cm = new ClassMethodMatcher("com.jitlogic.zorka.spy.*", "*", null);

        Assert.assertFalse(cm.matches("com.jitlogic.zorka.spy.unittest.SomeClass"));
        Assert.assertTrue(cm.matches("com.jitlogic.zorka.spy.AClass"));

    }

    @Test
    public void testClassMethodMatch() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "get*", null);

        Assert.assertTrue(cm.matches("test.SomeClass", "getVal"));
        Assert.assertFalse(cm.matches("test.SomeClass", "setVal"));
    }

    @Test
    public void testClassMethodStrictMatch() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "getVal", null);

        Assert.assertTrue(cm.matches("test.SomeClass", "getVal"));
        Assert.assertFalse(cm.matches("test.someClass", "getValAndSome"));
    }

    @Test
    public void testClassMatchSignatureWithoutTypes() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "getVal", null);

        Assert.assertTrue(cm.matches("test.someClass", "getVal", "()V"));
        Assert.assertTrue(cm.matches("test.someClass", "getVal", "(II)V"));
        Assert.assertFalse(cm.matches("test.someClass", "getVal", "malformed"));
    }

    @Test
    public void testClassMatchSignatureWithReturnVoidType() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "someMethod", "void");
        Assert.assertTrue(cm.matches("test.SomeClass", "someMethod", "()V"));
        Assert.assertFalse(cm.matches("test.SomeClass", "someMethod", "()Void"));
        Assert.assertFalse(cm.matches("test.SomeClass", "someMethod", "()I"));
    }

    @Test
    public void testClassMatchSignatureReturnClassType() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "getVal", "java.lang.String");

        Assert.assertTrue(cm.matches("test.SomeClass", "getVal", "()Ljava/lang/String;"));
    }

    @Test
    public void testClassMatchSignatureWithSimpleReturnAndArgumentType() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "frobnicate", "int", "int");

        Assert.assertTrue(cm.matches("test.someClass", "frobnicate", "(I)I"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(J)I"));
    }


    @Test
    public void testClassMatchSignatureWithStringType() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "frobnicate", "String", "String");

        Assert.assertTrue(cm.matches("test.someClass", "frobnicate", "(Ljava/lang/String;)Ljava/lang/String;"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(J)I"));
    }

    @Test
    public void testClassMatchWithVariousArgs() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "frobnicate",
                "String", "int", "com.jitlogic.zorka.spy.CallInfo");

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate",
                "(ILcom/jitlogic/zorka/spy/CallInfo;)Ljava/lang/String;"));
    }

    @Test
    public void testClassMatchWithNoArgsMarker() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "frobnicate", null, SpyDefinition.NO_ARGS);

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "()V"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(I)V"));
    }

    @Test
    public void testMatchWithMoreAttributesAndNoArgsFlag() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "frobnicate", null, "int", SpyDefinition.NO_ARGS);

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "(I)V"));
        Assert.assertFalse(cm.matches("test.SomeClass", "frobnicate", "(II)V"));
    }

    @Test
    public void testMatchWithJustSomeAttributes() {
        ClassMethodMatcher cm = new ClassMethodMatcher("test.*", "frobnicate", null, "int");

        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "(I)V"));
        Assert.assertTrue(cm.matches("test.SomeClass", "frobnicate", "(II)V"));
    }

}
