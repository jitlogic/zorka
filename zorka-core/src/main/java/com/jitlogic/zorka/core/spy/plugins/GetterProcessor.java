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
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Recursively fetches object attribute according to predefined attribute chain
 * using ObjectInspector.get() method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class GetterProcessor implements SpyProcessor {

    /**
     * Logger
     */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Source field
     */
    private String srcField;

    /**
     * Destination field
     */
    private String dstField;

    /**
     * Attribute chain
     */
    private Object[] attrChain;

    /**
     * Creates new getter processor.
     *
     * @param srcField  source field
     * @param dstField  destination field
     * @param attrChain attribute chain
     */
    public GetterProcessor(String srcField, String dstField, Object... attrChain) {
        this.srcField = srcField;
        this.dstField = dstField;
        this.attrChain = attrChain;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object val = ObjectInspector.get(record.get(srcField), attrChain);

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
            log.debug(ZorkaLogger.ZSP_ARGPROC, "Final result: '" + val + "' stored to slot " + dstField);
        }

        record.put(dstField, val);

        return record;
    }
}
