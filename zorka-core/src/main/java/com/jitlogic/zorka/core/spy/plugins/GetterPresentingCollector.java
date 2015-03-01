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

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.mbeans.AttrGetter;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.spy.SpyContext;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;


/**
 * Presents object as mbean attribute using ValGetter object.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class GetterPresentingCollector implements SpyProcessor {

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * MBean server registry
     */
    private MBeanServerRegistry registry;

    /**
     * MBean server name
     */
    private String mbsName;

    /**
     * MBean object name (or format template)
     */
    private String mbeanTemplate;

    /**
     * Attribute name (or format template)
     */
    private String attrTemplate;

    /**
     * Presented object description.
     */
    private String desc;

    /**
     * Source field
     */
    private String srcField;

    /**
     * Attribute chain used by ValGetter
     */
    private Object[] attrChain;


    /**
     * Creates presenting collector
     *
     * @param mbsName       mbean server name
     * @param mbeanTemplate object name
     * @param attrTemplate  attribute
     * @param desc          description
     * @param srcField      source field
     * @param attrChain     attribute chain
     */
    public GetterPresentingCollector(MBeanServerRegistry mbsRegistry, String mbsName, String mbeanTemplate,
                                     String attrTemplate, String desc, String srcField, Object... attrChain) {
        this.registry = mbsRegistry;
        this.mbsName = mbsName;
        this.mbeanTemplate = mbeanTemplate;
        this.attrTemplate = attrTemplate;
        this.srcField = srcField;
        this.desc = desc;
        this.attrChain = attrChain;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        SpyContext ctx = (SpyContext) record.get(".CTX");
        String mbeanName = ctx.subst(mbeanTemplate);
        String attrName = ctx.subst(attrTemplate);

        Object obj1 = new AttrGetter(record.get(srcField), attrChain);
        Object obj2 = registry.getOrRegister(mbsName, mbeanName, attrName, obj1, desc);

        if (!obj1.equals(obj2)) {
            log.warn(ZorkaLogger.ZSP_ERRORS, "Attribute '" + attrName + "' of '" + mbeanName + "' is already used.");
        }

        return record;
    }

}
