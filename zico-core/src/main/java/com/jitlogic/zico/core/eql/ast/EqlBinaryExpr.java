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

public class EqlBinaryExpr extends EqlOpExpr {


    public static EqlBinaryExpr make(Object arg2, Object opName, Object arg1) {
        EqlOp op = EqlOp.fromName(opName.toString());
        if (arg2 instanceof EqlBinaryExpr) {
            EqlBinaryExpr expr = (EqlBinaryExpr) arg2;
            if (!expr.isPreceding() && op.getPrecedence() <= expr.op.getPrecedence())
                return new EqlBinaryExpr(
                        new EqlBinaryExpr((EqlExpr) arg1, op, expr.arg1), expr.op, expr.arg2);
        }

        return new EqlBinaryExpr((EqlExpr) arg1, op, (EqlExpr) arg2);
    }


    private EqlOp op;
    private EqlExpr arg1, arg2;

    public EqlBinaryExpr(EqlExpr arg1, EqlOp op, EqlExpr arg2) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public EqlOp getOp() {
        return op;
    }


    public EqlExpr getArg1() {
        return arg1;
    }

    public void setArg1(EqlExpr arg1) {
        this.arg1 = arg1;
    }

    public EqlExpr getArg2() {
        return arg2;
    }

    public void setArg2(EqlExpr arg2) {
        this.arg2 = arg2;
    }

    public <T> T accept(EqlNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        return obj instanceof EqlBinaryExpr
                && op.equals(((EqlBinaryExpr) obj).op)
                && arg1.equals(((EqlBinaryExpr) obj).arg1)
                && arg2.equals(((EqlBinaryExpr) obj).arg2);
    }


    @Override
    public int hashCode() {
        return op.hashCode() + 31 * arg1.hashCode() + 31 * 31 * arg2.hashCode();
    }


    @Override
    public String toString() {
        return "(" + arg1 + " " + op.getName() + " " + arg2 + ")";
    }
}
