/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.mbeans.AttrGetter;
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.util.HashMap;
import java.util.Map;

import static com.jitlogic.zorka.spy.SpyLib.*;

/**
 * Collects  method call statistics and
 */
public class JmxAttrCollector implements SpyProcessor {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private final MBeanServerRegistry registry = AgentInstance.getMBeanServerRegistry();

    private final String mbsName;
    private final String beanTemplate;
    private final String attrTemplate;
    private final String time, tstamp;

    private final Map<SpyContext,MethodCallStatistic> cachedStats = new HashMap<SpyContext, MethodCallStatistic>();


    public JmxAttrCollector(String mbsName, String beanTemplate, String attrTemplate, String tstamp, String time) {
        this.mbsName = mbsName;
        this.beanTemplate = beanTemplate;
        this.attrTemplate = attrTemplate;
        this.time = time;
        this.tstamp = tstamp;
    }

    @Override
    public Map<String,Object> process(Map<String,Object> record) {

        if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
            log.debug("Collecting record: " + record);
        }

        SpyContext ctx = (SpyContext) record.get(".CTX");
        MethodCallStatistic statistic = cachedStats.get(ctx);

        if (statistic == null) {
            statistic = MethodCallStatistic.newStatAvg15(ctx.getMethodName()); // TODO make this configurable
            AttrGetter callsGetter = new AttrGetter(statistic, "calls");
            AttrGetter errorGetter = new AttrGetter(statistic, "errors");
            AttrGetter timeGetter = new AttrGetter(statistic, "time");
            AttrGetter lastGetter = new AttrGetter(statistic, "lastSample");

            String beanName = ctx.subst(beanTemplate);
            String attrName = ctx.subst(attrTemplate);

            String desc = ctx.getClassName() + "." + ctx.getMethodName();

            if (!callsGetter.equals(registry.getOrRegister(mbsName, beanName, attrName + "_calls", callsGetter,
                    desc + " calls"))) {
                log.warn("Cannot register attribute for " + desc + " calls. Atribute already taken.");
            }

            if (!errorGetter.equals(registry.getOrRegister(mbsName, beanName, attrName + "_errors", errorGetter,
                    desc + " errors"))) {
                log.warn("Cannot register attribute for " + desc + " errors.");
            }

            if (!timeGetter.equals(registry.getOrRegister(mbsName, beanName, attrName + "_time", timeGetter,
                    desc + " summary execution time"))) {
                log.warn("Cannot register attribute for " + desc + " summary execution time.");
            }

            if (!lastGetter.equals(registry.getOrRegister(mbsName, beanName, attrName + "_last", lastGetter,
                    desc + " last call (error) registered"))) {
                log.warn("Cannot register attribute for " + desc + " last call.");
            }
        } // if (stats == null)

        Object timeObj = record.get(time);
        Object tstampObj = record.get(tstamp);

        if (timeObj instanceof Long && tstampObj instanceof Long) {
            if (0 != ((Integer) record.get(".STAGES") & (1 << ON_RETURN))) {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("Logging record using logCall()");
                }
                statistic.logCall((Long) tstampObj, (Long) timeObj);
            } else if (0 != ((Integer) record.get(".STAGES") & (1 << ON_ERROR))) {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("Logging record using logError()");
                }
                statistic.logError((Long) tstampObj, (Long) timeObj);
            } else {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("No ON_RETURN nor ON_ERROR marked on record " + record);
                }
            }
        } else {
            if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                log.debug("Unknown type of time or tstamp object: " + timeObj);
            }
        }

        return record;
    }

}
