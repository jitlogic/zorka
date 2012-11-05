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
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;

/**
 * Performs string formating using values from current stage.
 */
public class StringFormatProcessor implements SpyProcessor {

    private int dst;
    private String expr;

    private ObjectInspector inspector = new ObjectInspector();

    public StringFormatProcessor(int dst, String expr) {
        this.dst = dst;
        this.expr = expr;
    }

    public SpyRecord process(int stage, SpyRecord record) {
        record.put(stage, dst, inspector.substitute(expr, record.getVals(stage)));
        return record;
    }
}
