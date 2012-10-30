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

package com.jitlogic.zorka.agent.testspy.debug;

import com.jitlogic.zorka.vmsci.MainSubmitter;
import org.junit.Test;

/**
 * This is not a test. This is used for looking how compiled code looks like.
 */
public class BytecodeSample1 {

    private Object x;
    private int xx;

    public BytecodeSample1(int xx) {
        x = this;
        this.xx = xx;
    }

    public void testCollectTrivialData() throws Exception {
        MainSubmitter.submit(1, 1, 1, null);
    }

    public void testSystemNanotime() throws Exception {
        Object[] v = new Object[1];
        v[0] = System.nanoTime();
        MainSubmitter.submit(1, 1, 1, null);
    }

    public void testCollectBasicTypes1(boolean bo, char ch) throws Exception {
        Object[] v = new Object[2];

        v[0] = bo;
        v[1] = ch;
    }

    public void testCollectBasicTypes2(int i, long l, short s, byte b) {
        Object[] v = new Object[4];

        v[0] = i;
        v[1] = l;
        v[2] = s;
        v[3] = b;
    }

}
