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

import java.util.ArrayList;
import java.util.List;

public class EqlFunCall extends EqlExpr {

    public static Object funcall(Object expr) {
        return new EqlFunCall((EqlExpr) expr);
    }

    public static Object argument(Object expr, Object funcall) {
        ((EqlFunCall) funcall).arguments.add((EqlExpr) expr);
        return funcall;
    }


    private EqlExpr function;

    private List<EqlExpr> arguments;


    public EqlFunCall(EqlExpr function, EqlExpr... arguments) {
        this.function = function;
        this.arguments = new ArrayList<EqlExpr>();

        for (EqlExpr arg : arguments) {
            this.arguments.add(arg);
        }
    }

    public EqlExpr getFunction() {
        return function;
    }

    public List<EqlExpr> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(function);

        sb.append('(');

        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(arguments.get(i));
        }

        sb.append(')');

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        return obj instanceof EqlFunCall
                && ((EqlFunCall) obj).function.equals(function)
                && ((EqlFunCall) obj).arguments.equals(arguments);
    }


    public int hashCode() {
        int hc = function.hashCode();

        for (EqlExpr e : arguments) {
            hc = 31 * hc + e.hashCode();
        }

        return hc;
    }

    @Override
    public <T> T accept(EqlNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
