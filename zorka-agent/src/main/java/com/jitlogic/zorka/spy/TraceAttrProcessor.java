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

package com.jitlogic.zorka.spy;

import java.util.Map;

public class TraceAttrProcessor implements SpyProcessor {

    private Tracer tracer;
    private String srcField;
    private int attrId;

    public TraceAttrProcessor(Tracer tracer, String srcField, String traceAttr) {
        this.tracer = tracer;
        this.srcField = srcField;
        this.attrId = tracer.getSymbolRegistry().symbolId(traceAttr);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        tracer.getHandler().newAttr(attrId, record.get(srcField));
        return record;
    }
}
