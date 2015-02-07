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

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TaggedValue;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.*;


public class TraceTaggerProcessor implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private Tracer tracer;
    private int attrNameId, attrTagId;
    private Set<Integer> traceTagIds;


    public TraceTaggerProcessor(SymbolRegistry symbolRegistry, Tracer tracer, String attrName, String attrTag, String... traceTags) {
        this.tracer = tracer;
        this.attrNameId = symbolRegistry.symbolId(attrName);
        this.attrTagId = symbolRegistry.symbolId(attrTag);

        traceTagIds = new HashSet<Integer>(traceTags.length);

        for (String traceTag : traceTags) {
            traceTagIds.add(symbolRegistry.symbolId(traceTag));
        }
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        Object tagObj = tracer.getHandler().getAttr(attrNameId);

        if (tagObj == null) {
            tagObj = new TaggedValue(attrTagId, new HashSet<Integer>());
            tracer.getHandler().newAttr(-1, attrNameId, tagObj);
        }

        if (!(tagObj instanceof TaggedValue) || ((TaggedValue) tagObj).getTagId() != attrTagId) {
            log.error(ZorkaLogger.ZSP_ERRORS, "Trace Tag attribute already occupied with different object: " + record);
        }

        Set<Integer> tagSet = (Set<Integer>) ((TaggedValue) tagObj).getValue();
        tagSet.addAll(traceTagIds);

        return record;
    }
}
