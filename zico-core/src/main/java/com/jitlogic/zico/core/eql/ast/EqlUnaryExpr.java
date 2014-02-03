/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zico.core.eql.EqlNodeVisitor;


public class EqlUnaryExpr extends EqlOpExpr {

    private EqlOp op;
    private EqlExpr arg;

    public static Object make(Object arg, Object opName) {
        EqlOp op = EqlOp.fromName(opName.toString());
        if (arg instanceof EqlBinaryExpr) {
            EqlBinaryExpr expr = (EqlBinaryExpr) arg;
            if (!expr.isPreceding() && op.getPrecedence() <= expr.getOp().getPrecedence())
                return new EqlBinaryExpr(
                        new EqlUnaryExpr(op, expr.getArg1()), expr.getOp(), expr.getArg2());
        }

        return new EqlUnaryExpr(op, (EqlExpr) arg);
    }

    public EqlUnaryExpr(EqlOp op, EqlExpr arg) {
        this.op = op;
        this.arg = arg;
    }

    @Override
    public <T> T accept(EqlNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }


    public EqlOp getOp() {
        return op;
    }


    public EqlExpr getArg() {
        return arg;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        return obj instanceof EqlUnaryExpr
                && ((EqlUnaryExpr) obj).op.equals(op)
                && ((EqlUnaryExpr) obj).arg.equals(arg);
    }


    @Override
    public int hashCode() {
        return op.hashCode() + 31 * arg.hashCode();
    }


    @Override
    public String toString() {
        return "(" + op.toString() + " " + arg.toString() + ")";
    }

}
