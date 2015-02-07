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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Performs string formating using values from current stage.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class StringFormatProcessor implements SpyProcessor {

    /**
     * Destination field
     */
    private String dstField;

    /**
     * Format expression
     */
    private String expr;

    /**
     * Maximum length
     */
    private int len;

    /**
     * Creates new string format processor.
     *
     * @param dstField destination field
     * @param expr     expression
     */
    public StringFormatProcessor(String dstField, String expr, int len) {
        this.dstField = dstField;
        this.expr = expr;
        this.len = len;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        String s = ObjectInspector.substitute(expr, record);

        if (len > 0 && s.length() > len) {
            s = s.substring(0, len);
        }

        record.put(dstField, s);

        return record;
    }
}
