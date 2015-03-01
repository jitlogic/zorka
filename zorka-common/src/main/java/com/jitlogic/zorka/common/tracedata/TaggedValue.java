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

/**
 * Tagged values can be used as attribute values in trace data.
 * Tagging value makes custom processing at collector side possible.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TaggedValue {

    /**
     * Tag name symbol ID
     */
    private int tagId;

    /**
     * Tagged value
     */
    private Object value;


    /**
     * Creates new tagged value object
     *
     * @param tagId tag name (symbol ID)
     * @param value tagged value
     */
    public TaggedValue(int tagId, Object value) {
        this.tagId = tagId;
        this.value = value;
    }

    public int getTagId() {
        return tagId;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TaggedValue
                && tagId == ((TaggedValue) obj).tagId
                && ZorkaUtil.objEquals(value, ((TaggedValue) obj).value);
    }


    @Override
    public int hashCode() {
        return tagId + (value != null ? value.hashCode() : 0);
    }


    @Override
    public String toString() {
        return value != null ? value.toString() : "null";
    }
}
