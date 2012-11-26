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
import com.jitlogic.zorka.spy.SpyCollector;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.HashMap;
import java.util.Map;

import static com.jitlogic.zorka.spy.SpyConst.*;
import static com.jitlogic.zorka.spy.SpyLib.*;

public class ZorkaStatsCollector implements SpyCollector {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private MBeanServerRegistry registry = AgentInstance.getMBeanServerRegistry();
    private String mbsName, mbeanTemplate, attrTemplate, keyTemplate;
    private int timeField, tstampField;

    private Map<SpyContext,MethodCallStatistics> statsCache = new HashMap<SpyContext, MethodCallStatistics>();

    private ObjectInspector inspector = new ObjectInspector();

    public ZorkaStatsCollector(String mbsName, String mbeanTemplate, String attrTemplate,
                               String keyTemplate, int tstampField, int timeField) {
        this.mbsName  = mbsName;
        this.mbeanTemplate = mbeanTemplate;
        this.attrTemplate = attrTemplate;
        this.keyTemplate = keyTemplate;
        this.tstampField = tstampField;
        this.timeField = timeField;
    }


    public void collect(SpyRecord record) {

        if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
            log.debug("Collecting record: [" + ZorkaUtil.join(",", record.getVals(ON_COLLECT)) + "]");
        }

        SpyContext ctx = record.getContext();
        MethodCallStatistics stats = statsCache.get(record.getContext());

        if (stats == null) {
            stats = registry.getOrRegister(mbsName, subst(mbeanTemplate, ctx), subst(attrTemplate, ctx),
                    new MethodCallStatistics(), "Method call statistics");
        }

        String key = inspector.substitute(subst(keyTemplate, ctx), record.getVals(ON_COLLECT));

        MethodCallStatistic statistic = (MethodCallStatistic)stats.getMethodCallStatistic(key);

        Object timeObj = timeField >= 0 ? record.get(ON_COLLECT, timeField) : 0L;
        Object tstampObj = tstampField >= 0 ? record.get(ON_COLLECT, tstampField) : 0L;

        if (timeObj instanceof Long && tstampObj instanceof Long) {
            if (record.gotStage(ON_RETURN)) {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("Logging record using logCall()");
                }
                statistic.logCall((Long)tstampObj, (Long)timeObj);
            } else if (record.gotStage(ON_ERROR)) {
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
    }


    private String subst(String template, SpyContext ctx) {
        return template
                .replace("${className}", ctx.getClassName())
                .replace("${methodName}", ctx.getMethodName())
                .replace("${shortClassName}", ctx.getShortClassName());
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


    public int getTimeField() {
        return timeField;
    }


    public int getTstampField() {
        return tstampField;
    }


    public String getKeyTemplate() {
        return keyTemplate;
    }
}
