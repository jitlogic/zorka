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
package com.jitlogic.zico.core.eql;


import com.jitlogic.zico.core.eql.ast.*;

public class EqlNodeVisitor<T> {

    public T visit(EqlExpr node) {
        return node.accept(this);
    }

    public T visit(EqlFunCall node) {
        for (EqlExpr expr : node.getArguments()) {
            expr.accept(this);
        }
        node.getFunction().accept(this);
        return null;
    }

    public T visit(EqlBinaryExpr node) {
        node.getArg1().accept(this);
        node.getArg2().accept(this);
        return null;
    }

    public T visit(EqlUnaryExpr node) {
        node.getArg().accept(this);
        return null;
    }

    public T visit(EqlLiteral node) {
        return null;
    }

    public T visit(EqlSymbol node) {
        return null;
    }

}
