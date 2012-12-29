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

package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.mbeans.AttrGetter;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;


/**
 * Presents object as mbean attribute using ValGetter object.
 */
public class GetterPresentingCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private MBeanServerRegistry registry = AgentInstance.getMBeanServerRegistry();

    private String mbsName;
    private String mbeanTemplate, attrTemplate;
    private String desc;
    private String src;
    private Object[] path;


    public GetterPresentingCollector(String mbsName, String mbeanTemplate, String attrTemplate, String desc,
                                     String src, Object...path) {
        this.mbsName = mbsName;
        this.mbeanTemplate = mbeanTemplate;
        this.attrTemplate = attrTemplate;
        this.src = src;
        this.desc = desc;
        this.path = path;
    }


    public SpyRecord process(SpyRecord record) {
        SpyContext ctx = record.getContext();
        String mbeanName = ctx.subst(mbeanTemplate);
        String attrName = ctx.subst(attrTemplate);

        Object obj1 = new AttrGetter(record.get(src), path);
        Object obj2 = registry.getOrRegister(mbsName, mbeanName, attrName, obj1, desc);

        if (obj1.equals(obj2)) {
            log.warn("Attribute '" + attrName + "' of '" + mbeanName + "' is already used.");
        }

        return record;
    }

}
