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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import java.util.Map;

public class TraceFlagsProcessor implements SpyProcessor {

    /**
     * Tracer object
     */
    private Tracer tracer;

    /**
     * Trace ID (if any)
     */
    private int traceId;

    /**
     * source field name
     */
    private String checkField;

    /**
     * flags to be set
     */
    private int flags;

    public TraceFlagsProcessor(Tracer tracer, String checkField, int traceId, int flags) {
        this.tracer = tracer;
        this.checkField = checkField;
        this.flags = flags;
        this.traceId = traceId;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        if (checkField == null || record.get(checkField) != null) {
            tracer.getHandler().markTraceFlags(traceId, flags);
        }

        return record;
    }
}
