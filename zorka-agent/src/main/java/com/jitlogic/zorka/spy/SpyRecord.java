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

package com.jitlogic.zorka.spy;

import static com.jitlogic.zorka.spy.SpyLib.*;

public class SpyRecord {

    private final static Object[] NO_VALS = { };

    private SpyContext ctx;

    private Object[][] vals = { NO_VALS, NO_VALS, NO_VALS, NO_VALS, NO_VALS };

    private int stages = 0;


    public SpyRecord(SpyContext ctx) {
        this.ctx = ctx;
    }


    public SpyRecord feed(int stage, Object[] vals) {
        this.vals[stage] = vals;

        stages |= (1 << stage);

        return this;
    }


    public boolean gotStage(int stage) {
        return 0 != (stages & (1 << stage));
    }


    public void cleanup() {
        vals[ON_ENTER] = NO_VALS;
        vals[ON_RETURN] = NO_VALS;
        vals[ON_ERROR] = NO_VALS;
        vals[ON_SUBMIT] = NO_VALS;
    }

    public SpyContext getContext() {
        return ctx;
    }


    public int size(int stage) {
        return vals[stage].length;
    }


    public Object get(int stage, int idx) {
        return vals[stage][idx];
    }

    public Object[] getVals(int stage) {
        return vals[stage];
    }

    public void put(int stage, int idx, Object v) {
        Object[] vs = vals[stage];
        if (idx >= vs.length) {
            vs = new Object[idx+1];
            System.arraycopy(vals[stage], 0, vs, 0, vals[stage].length);
            vals[stage] = vs;
        }
        vs[idx] = v;
    }
}
