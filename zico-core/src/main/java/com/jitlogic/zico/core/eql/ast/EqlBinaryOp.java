/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.core.eql.ast;


import java.util.HashMap;
import java.util.Map;

public enum EqlBinaryOp {

    DOT(".", 1),
    BIT_NOT("~", 2),
    MUL("*", 3),
    DIV("/", 3),
    REM("%", 3),
    ADD("+", 4),
    SUB("-", 4),
    BIT_AND("&", 5),
    BIT_OR("|", 6),
    BIT_XOR("^", 6),
    EQ("=", 7),
    NE("<>", 7),
    RE("~=", 7),
    LT("<", 7),
    LE("<=", 7),
    GT(">", 7),
    GE(">=", 7),
    IS("is", 7),
    NOT("not", 8),
    AND("and", 9),
    OR("or", 10);


    private final String name;
    private final int precedence;

    private EqlBinaryOp(String op, int precedence) {
        this.name = op;
        this.precedence = precedence;
    }

    public String getName() {
        return name;
    }

    public int getPrecedence() {
        return precedence;
    }

    private static Map<String, EqlBinaryOp> operations = new HashMap<String, EqlBinaryOp>();

    public static EqlBinaryOp fromName(String name) {
        if (!operations.containsKey(name)) {
            throw new RuntimeException("Illegal operator: " + name);
        }
        return operations.get(name);
    }

    static {

        for (EqlBinaryOp op : EqlBinaryOp.values()) {
            operations.put(op.getName(), op);
        }

        operations.put("!=", NE);
        operations.put("&&", AND);
        operations.put("||", OR);
    }
}
