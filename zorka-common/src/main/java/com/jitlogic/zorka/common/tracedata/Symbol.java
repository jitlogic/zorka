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
package com.jitlogic.zorka.common.tracedata;


import com.jitlogic.zorka.common.util.ZorkaUtil;

public class Symbol {
    private final int id;
    private final String name;

    public Symbol(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Symbol &&
            ((Symbol)obj).id == id &&
            ZorkaUtil.objEquals(name, ((Symbol)obj).name);
    }

    @Override
    public String toString() {
        return "Symbol(" + id + "," + name + ")";
    }

    @Override
    public int hashCode() {
        return 17 * id + 31 * (name != null ? name.hashCode() : 0);
    }
}
