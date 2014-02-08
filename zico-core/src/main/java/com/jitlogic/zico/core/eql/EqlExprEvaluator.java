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
package com.jitlogic.zico.core.eql;


import com.jitlogic.zico.core.eql.ast.EqlBinaryExpr;
import com.jitlogic.zico.core.eql.ast.EqlLiteral;
import com.jitlogic.zico.core.eql.ast.EqlSymbol;
import com.jitlogic.zico.core.eql.ast.EqlUnaryExpr;
import com.jitlogic.zorka.common.util.ZorkaUtil;

public abstract class EqlExprEvaluator extends EqlNodeVisitor<Object> {


    public Object visit(EqlBinaryExpr expr) {

        switch (expr.getOp()) {
            case EQ:
                return EqlUtils.objEquals(expr.getArg1().accept(this), expr.getArg2().accept(this));
            case NE:
                return !EqlUtils.objEquals(expr.getArg1().accept(this), expr.getArg2().accept(this));
            case RE:
                if (expr.getArg2().getClass() == EqlLiteral.class) {
                    // Optimization: precompile pattern if constant
                    EqlLiteral lit = (EqlLiteral) expr.getArg2();
                    lit.setValue(EqlUtils.compile(lit.getValue()));
                }
                return EqlUtils.regex(expr.getArg1().accept(this), expr.getArg2().accept(this));
            case GT:
                return EqlUtils.compare(expr.getArg1().accept(this), expr.getArg2().accept(this)) > 0;
            case LT:
                return EqlUtils.compare(expr.getArg1().accept(this), expr.getArg2().accept(this)) < 0;
            case GE:
                return EqlUtils.compare(expr.getArg1().accept(this), expr.getArg2().accept(this)) >= 0;
            case LE:
                return EqlUtils.compare(expr.getArg1().accept(this), expr.getArg2().accept(this)) <= 0;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case REM:
                return EqlUtils.arithmetic(expr.getArg1().accept(this), expr.getOp(), expr.getArg2().accept(this));
            case AND:
            case OR:
                return EqlUtils.logical(expr.getArg1().accept(this), expr.getOp(), expr.getArg2().accept(this));
            case BIT_AND:
            case BIT_OR:
            case BIT_XOR:
                return EqlUtils.bitwise(expr.getArg1().accept(this), expr.getOp(), expr.getArg2().accept(this));
        }

        throw new EqlException("Operation '" + expr.getOp().getName() + "' not suitable for binary expressions.", expr);
    }


    public Object visit(EqlUnaryExpr expr) {
        switch (expr.getOp()) {
            case NOT:
                return !ZorkaUtil.coerceBool(expr.getArg().accept(this));
            case BIT_NOT:
                return EqlUtils.negate(expr.getArg().accept(this));
        }

        throw new EqlException("Operation '" + expr.getOp().getName() + "' not suitable for unary expressions.", expr);
    }


    public Object visit(EqlSymbol node) {
        return resolve(node.getName());
    }


    public Object visit(EqlLiteral node) {
        return node.getValue();
    }


    protected abstract Object resolve(String name);

}
