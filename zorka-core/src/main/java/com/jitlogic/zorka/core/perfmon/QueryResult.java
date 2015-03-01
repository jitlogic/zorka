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

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.*;


/**
 * Represents one item of JMX query result set.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class QueryResult {

    /** Final result value */
    private Object value;

    /** Attribute map */
    private Map<String,Object> attrs;


    /**
     * Wrapss value into query result object.
     *
     * @param value wrapped value
     */
    public QueryResult(Object value) {
        this.value = value;
    }


    /**
     * Creates new variant of query result (with new result value)
     *
     * @param orig original result
     *
     * @param value new value
     */
    public QueryResult(QueryResult orig, Object value) {
        this.value = value;
        if (orig.attrs != null) {
            attrs = new LinkedHashMap<String, Object>();
            attrs.putAll(orig.attrs);
        }
    }


    /**
     * Sets result attribute
     *
     * @param key attribute key
     *
     * @param val attribute value
     */
    public void setAttr(String key, Object val) {
        if (attrs == null) {
            attrs = new LinkedHashMap<String, Object>();
        }
        attrs.put(key, val);
    }


    /**
     * Returns result attribute
     *
     * @param key attribute key
     *
     * @return attribute value or null
     */
    public Object getAttr(String key) {
        return attrs != null ? attrs.get(key) : null;
    }


    public Map<String,Object> getAttrs() {
        return attrs;
    }


    /**
     * Returns attribute map (or empty map if object has no attributes)
     *
     * @return result attribute map
     */
    public Set<Map.Entry<String,Object>> attrSet() {
        return attrs != null ? attrs.entrySet() : new HashSet<Map.Entry<String, Object>>(1);
    }


    /**
     * Creates a string by concatenating all attribute values and registers it as a symbol.
     *
     * @param symbols symbol registry
     *
     * @return symbol ID
     */
    public int getComponentId(SymbolRegistry symbols) {
        String path = getAttrPath();

        return symbols.symbolId(path);
    }


    /**
     * Returns "attribute path" - all attribute values concatenated using '.' character.
     * @return
     */
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


    public String getKey(Set<String> omitAttrs) {
        StringBuilder sb = new StringBuilder();

        if (attrs != null) {
            for (Map.Entry<String,Object> e : attrs.entrySet()) {
                if (!omitAttrs.contains(e.getKey())) {
                    if (sb.length() != 0) {
                        sb.append('.');
                    }
                    sb.append(e.getValue());
                }
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
