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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Compares two fields or a field with constant value.
 * Records are dropped when comparison result is false.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ComparatorProcessor implements SpyProcessor {

    /**
     * Accuracy when comparing floating point values
     */
    private static final double ACCURACY = 0.001;

    /**
     * Greater-than operator
     */
    private static final int GT = 0;

    /**
     * Greater-or-equal operator
     */
    private static final int GE = 1;

    /**
     * Equal operator
     */
    private static final int EQ = 2;

    /**
     * Less-or-equal operator
     */
    private static final int LE = 3;

    /**
     * Less-than operator
     */
    private static final int LT = 4;

    /**
     * Not-equal operator
     */
    private static final int NE = 5;

    /**
     * Decision table for comaprison operators
     */
    private static final boolean[][] ctab = {
            {true, false, false}, // GT
            {true, true, false}, // GE
            {false, true, false}, // EQ
            {false, true, true}, // LE
            {false, false, true}, // LT
            {true, false, true}, // NE
    };

    /**
     * Operator name to ID map
     */
    private static final Map<String, Integer> operators = ZorkaUtil.constMap(
            ">", GT, ">=", GE, "=", EQ, "==", EQ, "<=", LE, "<", LT, "!=", NE, "<>", NE);


    /**
     * Creates comparator that compares two fields: a op b
     *
     * @param a  first field name
     * @param op operator
     * @param b  second field name
     * @return comparator filtering processor
     */
    public static ComparatorProcessor scmp(String a, String op, String b) {
        return new ComparatorProcessor(a, operators.get(op), b, null);
    }


    /**
     * Creates comparator that compares field with a constant: a op v
     *
     * @param a  first field name
     * @param op operator
     * @param v  constant value
     * @return comparator filtering processor
     */
    public static ComparatorProcessor vcmp(String a, String op, Object v) {
        return new ComparatorProcessor(a, operators.get(op), null, v);
    }

    /**
     * First operand
     */
    private String a;

    /**
     * Second operand (if it is field)
     */
    private String b;

    /**
     * Operator ID
     */
    private int op;

    /**
     * Second operand (if it is constant)
     */
    private Object v;

    /**
     * Constructor is hidden. Use scmp() and vcmp() static methods instead.
     *
     * @param a  first operand
     * @param op operator
     * @param b  second operand (field)
     * @param v  second operand (constant)
     */
    private ComparatorProcessor(String a, int op, String b, Object v) {
        this.a = a;
        this.op = op;
        this.b = b;
        this.v = v;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object va = record.get(a);
        Object vb = (b != null) ? record.get(b) : v;

        if (va instanceof Number && vb instanceof Number) {
            int rcmp;
            if (va instanceof Double || va instanceof Float || vb instanceof Double || vb instanceof Float) {
                double da = ((Number) va).doubleValue(), db = ((Number) vb).doubleValue();
                double ac = Math.max(da, db) * ACCURACY;
                rcmp = Math.abs(db - da) < ac ? 0 : (da > db ? -1 : 1);
            } else {
                long la = ((Number) va).longValue(), lb = ((Number) vb).longValue();
                rcmp = la == lb ? 0 : la > lb ? -1 : 1;
            }

            return ctab[op][rcmp + 1] ? record : null;
        } else if (op == EQ || op == NE) {
            return ((va == null && vb == null) || (va != null && va.equals(vb))) ? record : null;
        }

        return null;
    }

}
