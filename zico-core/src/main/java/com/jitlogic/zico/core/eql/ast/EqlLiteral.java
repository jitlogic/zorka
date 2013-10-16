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


import com.jitlogic.zorka.common.util.ZorkaUtil;

public class EqlLiteral extends EqlExpression {

    private Object value;

    public EqlLiteral(Object val) {
        this.value = val;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value instanceof String ? "'" + value + "'" : value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EqlLiteral
                && ZorkaUtil.objEquals(((EqlLiteral) obj).value, value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public <T> T accept(EqlNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
