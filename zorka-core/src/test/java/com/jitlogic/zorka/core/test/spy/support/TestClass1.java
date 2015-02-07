
/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.spy.support;

@ClassAnnotation
public class TestClass1 {

    private static int scalls = 0;
    private int calls = 0;
    private int vals = 0;
    private String tag = "";

    public void trivialMethod() {
        calls++;
    }

    public void trivialStrMethod(String tag) {
        calls++;
        this.tag = tag;
    }

    public void errorMethod() {
        throw new NullPointerException("dUP!");
    }

    static void nonPublicStatic() {
        scalls++;
    }

    public void paramMethod1(int i, long j, short s, byte b) {
        calls++;
        vals += i + j + s + b;
    }

    public void paramMethod2(boolean b, char c) {
        vals += (b ? 1 : 0) + (byte)c;
    }

    public void paramMethod3(double d, float f) {
        vals += (int)(d*100) + (int)(f*10);
    }

    public void paramMethod4(int[] a, byte[] b, double[] c) {
        vals += a.length + b.length + c.length;
    }

    public String strMethod() {
        return "oja!";
    }

    public int getUltimateQuestionOfLife() {
        return 42;
    }

    public int getUltimateQuestionWithLocalVars() {
        String s = "oja!";
        return 38 + s.length();
    }

    public int getCalls() {
        return calls;
    }

    public String getS() {
        return tag;
    }


    public Number complicatedMethod(String s) {
        Number rslt = null;
        if (s.endsWith("L")) {
            rslt = Long.parseLong(s.substring(0, s.length()-1));
        } else {
            rslt = Integer.parseInt(s);
        }
        return rslt;
    }

}
