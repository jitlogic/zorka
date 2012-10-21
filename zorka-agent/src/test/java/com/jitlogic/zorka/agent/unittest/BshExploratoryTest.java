/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.agent.unittest;

import bsh.Interpreter;
import bsh.NameSpace;
import bsh.Parser;
import bsh.This;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Various little tests exploring various aspects of bsh interpreter.
 */
public class BshExploratoryTest {

    private Interpreter interpreter;
    private Object result = null;

    public void catchResult(Object result) {
        this.result = result;
    }

    @Before
    public void setUp() {
        interpreter = new Interpreter();
        interpreter.set("test", this);
    }

    @Test
    public void testBshMethodObjectClass() throws Exception {
        interpreter.eval("__ns() { x = 0; fun(i) { x += i; return x; } return this; }\n ns = __ns();");
        interpreter.eval("test.catchResult(ns);");
        Assert.assertTrue("should return NameSpace", result instanceof This);
    }

    @Test
    public void testBshThisObjectType() throws Exception {
        interpreter.eval("test.catchResult(this);");
        Assert.assertTrue("should return NameSpace", result instanceof This);
    }

}
