/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.util.ObjectInspector;

import java.util.Map;

/**
 * Performs string formating using values from current stage.
 */
public class StringFormatProcessor implements SpyProcessor {

    private String dst;
    private String expr;

    public StringFormatProcessor(String dst, String expr) {
        this.dst = dst;
        this.expr = expr;
    }

    public Map<String,Object> process(Map<String,Object> record) {
        record.put(dst, ObjectInspector.substitute(expr, record));
        return record;
    }
}
