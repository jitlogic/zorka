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
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.HashMap;
import java.util.Map;

import static com.jitlogic.zorka.spy.SpyLib.*;

public class ZorkaStatsCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private MBeanServerRegistry registry = AgentInstance.getMBeanServerRegistry();
    private String mbsName, mbeanTemplate, attrTemplate, keyTemplate;
    private String time, tstamp;

    private Map<SpyContext,MethodCallStatistics> statsCache = new HashMap<SpyContext, MethodCallStatistics>();

    private ObjectInspector inspector = new ObjectInspector();

    public ZorkaStatsCollector(String mbsName, String mbeanTemplate, String attrTemplate,
                               String keyTemplate, String tstamp, String time) {
        this.mbsName  = mbsName;
        this.mbeanTemplate = mbeanTemplate;
        this.attrTemplate = attrTemplate;
        this.keyTemplate = keyTemplate;
        this.time = time;
        this.tstamp = tstamp;
    }


    public SpyRecord process(SpyRecord record) {

        if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
            log.debug("Collecting record: " + record);
        }

        SpyContext ctx = record.getContext();
        MethodCallStatistics stats = statsCache.get(record.getContext());

        if (stats == null) {
            stats = registry.getOrRegister(mbsName, ctx.subst(mbeanTemplate), ctx.subst(attrTemplate),
                    new MethodCallStatistics(), "Method call statistics");
        }

        String key = inspector.substitute(ctx.subst(keyTemplate), record);

        MethodCallStatistic statistic = (MethodCallStatistic)stats.getMethodCallStatistic(key);

        Object timeObj = time != null ? record.get(time) : 0L;  // TODO WTF?
        Object tstampObj = tstamp != null ? record.get(tstamp) : 0L;   // TODO WTF?

        if (timeObj instanceof Long && tstampObj instanceof Long) {
            if (record.hasStage(ON_RETURN)) {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("Logging record using logCall()");
                }
                statistic.logCall((Long)tstampObj, (Long)timeObj);
            } else if (record.hasStage(ON_ERROR)) {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("Logging record using logError()");
                }
                statistic.logError((Long)tstampObj, (Long)timeObj);
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


    public String getMbsName() {
        return mbsName;
    }


    public String getMbeanTemplate() {
        return mbeanTemplate;
    }


    public String getAttrTemplate() {
        return attrTemplate;
    }



    public String getKeyTemplate() {
        return keyTemplate;
    }
}
