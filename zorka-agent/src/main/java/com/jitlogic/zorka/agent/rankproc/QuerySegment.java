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

package com.jitlogic.zorka.agent.rankproc;


import java.util.regex.Pattern;

public class QuerySegment {

    public static final int OBJECT_PART    = 0;
    public static final int COMPONENT_PART = 1;

    private int part;
    private Object attr;
    private String name;

    public QuerySegment(int part, Object attr) {
        this(part, attr, null);
    }

    public QuerySegment(int part, Object attr, String name) {
        this.part = part;
        this.attr = attr;
        this.name = name;
    }


    public int getPart() {
        return part;
    }


    public Object getAttr() {
        return attr;
    }


    public String getName() {
        return name;
    }


    public boolean matches(Object v) {
        return v != null && attr instanceof Pattern
                ? ((Pattern)attr).matcher(v.toString()).matches()
                : attr.equals(v);
    }
}
