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

import com.jitlogic.zorka.common.SymbolRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryResult {

    private Object value;

    private Map<String,Object> attrs;


    public QueryResult(Object value) {
        this.value = value;
    }


    public QueryResult(QueryResult orig, Object value) {
        this.value = value;
        if (orig.attrs != null) {
            attrs = new LinkedHashMap<String, Object>();
            attrs.putAll(orig.attrs);
        }
    }


    public void setAttr(String key, Object val) {
        if (attrs == null) {
            attrs = new LinkedHashMap<String, Object>();
        }
        attrs.put(key, val);
    }


    public Object getAttr(String key) {
        return attrs != null ? attrs.get(key) : null;
    }


    public int getComponentId(SymbolRegistry symbols) {
        String path = getAttrPath();

        return symbols.symbolId(path);
    }


    public String getAttrPath() {
        StringBuilder sb = new StringBuilder();

        if (attrs != null) {
            for (Map.Entry<String,Object> e : attrs.entrySet()) {
                if (sb.length() != 0) {
                    sb.append('.');
                }
                sb.append(e.getValue());
            }
        }

        return sb.toString();
    }


    public void setValue(Object value) {
        this.value = value;
    }


    public Object getValue() {
        return value;
    }


}
