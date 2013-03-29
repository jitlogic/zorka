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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.util.ObjectInspector;

import java.util.Map;

/**
 * Performs string formating using values from current stage.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class StringFormatProcessor implements SpyProcessor {

    /** Destination field */
    private String dstField;

    /** Format expression */
    private String expr;

    /**
     * Creates new string format processor.
     *
     * @param dstField destination field
     *
     * @param expr expression
     */
    public StringFormatProcessor(String dstField, String expr) {
        this.dstField = dstField;
        this.expr = expr;
    }

    @Override
    public Map<String,Object> process(Map<String,Object> record) {
        record.put(dstField, ObjectInspector.substitute(expr, record));
        return record;
    }
}
