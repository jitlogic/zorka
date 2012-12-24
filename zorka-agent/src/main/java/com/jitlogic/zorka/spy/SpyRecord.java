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

import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.Map;

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


    public SpyContext getContext() {
        return ctx;
    }


    public int size() {
        int size = 0;
        for (Object[] stv : vals) {
            size += stv.length;
        }
        return size;
    }


    public Object get(String key) {
        int[] slot = slot(key);
        return vals[slot[0]][slot[1]];
    }


    public void put(String key, Object val) {
        int[] slot = slot(key);
        Object[] vs = vals[slot[0]];
        if (slot[1] >= vs.length) {
            vs = new Object[slot[1] +1];
            System.arraycopy(vals[slot[0]], 0, vs, 0, vals[slot[0]].length);
            vals[slot[0]] = vs;
        }
        vs[slot[1]] = val;
    }


    private static Map<Character,Integer> stageNames = ZorkaUtil.constMap(
            'E', ON_ENTER, 'e', ON_ENTER,
            'R', ON_RETURN, 'r', ON_RETURN,
            'X', ON_ERROR, 'x', ON_ERROR,
            'S', ON_SUBMIT, 's', ON_SUBMIT,
            'C', ON_COLLECT, 's', ON_COLLECT
    );


    private static int[] slot(Object v) {
        if (v instanceof Integer) {
            return new int[] {-1, (Integer)v};
        } else if (v instanceof String) {
            String s = (String)v;
            Integer stage = stageNames.get(s.charAt(0));
            if (stage == null) {
                throw new IllegalArgumentException("Invalid stage. Try using one of characters: EeRrXxSsCc");
            }
            return new int[] { stage, Integer.parseInt(s.substring(1))};
        } else {
            throw new IllegalArgumentException("Illegal slot argument: " + v);
        }
    } // slot()

}
