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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.api.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;

public class ComparatorProcessor implements SpyProcessor {

    private final double ACCURACY = 0.001;

    private final boolean[][] ctab = {
            { true,  false, false }, // GT
            { true,  true,  false }, // GE
            { false, true,  false }, // EQ
            { false, true,  true  }, // LE
            { false, false, true  }, // LT
            { true,  false, true  }, // NE
    };

    public static ComparatorProcessor scmp(String a, int op, String b) {
        return new ComparatorProcessor(a, op, b, null);
    }

    public static ComparatorProcessor vcmp(String a, int op, Object v) {
        return new ComparatorProcessor(a, op, null, v);
    }


    private String a, b;
    private int op;
    private Object v;


    public ComparatorProcessor(String a, int op, String b, Object v) {
        this.a = a;
        this.op = op;
        this.b = b;
        this.v = v;
    }


    public SpyRecord process(SpyRecord record) {
        Object va = record.get(a);
        Object vb = (b != null) ? record.get(b) : v;

        if (va instanceof Number && vb instanceof Number) {
            int rcmp;
            if (va instanceof Double || va instanceof Float || vb instanceof Double || vb instanceof Float) {
                double da = ((Number)va).doubleValue(), db = ((Number)vb).doubleValue();
                double ac = Math.max(da, db) * ACCURACY;
                rcmp = Math.abs(db - da) < ac ? 0 : (da > db ? -1 : 1);
            } else {
                long la = ((Number)va).longValue(), lb = ((Number)vb).longValue();
                rcmp =  la == lb ? 0 : la > lb ? -1 : 1;
            }

            return ctab[op][rcmp+1] ? record : null;
        } else  if (op == SpyLib.EQ || op == SpyLib.NE) {
            return ((va == null && vb == null) || (va != null && va.equals(vb))) ? record : null;
        }

        return null;
    }

}
