
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

package com.jitlogic.zorka.agent.testspy.support;


public class TestClass1 {

    private int calls = 0;
    private int vals = 0;

    public void trivialMethod() {
        calls++;
    }


    public void errorMethod() {
        throw new NullPointerException("dUP!");
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

    public int getCalls() {
        return calls;
    }
}
