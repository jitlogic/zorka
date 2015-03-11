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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.Map;

public class TraceAttrGetterProcessor  implements SpyProcessor {

    private String dstField;

    private int traceId;

    private int attrId;

    /**
     * Tracer object
     */
    private Tracer tracer;

    public TraceAttrGetterProcessor(Tracer tracer, String dstField, int traceId, int attrId) {
        this.tracer = tracer;
        this.dstField = dstField;
        this.traceId = traceId;
        this.attrId = attrId;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        record.put(dstField, tracer.getHandler().getAttr(traceId, attrId));

        return record;
    }
}
