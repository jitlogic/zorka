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

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.util.HashMap;
import java.util.Map;

import static com.jitlogic.zorka.spy.SpyLib.*;

/**
 * Maintains statistics about method calls and updates them using data from incoming records.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaStatsCollector implements SpyProcessor {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaStatsCollector.class);

    /** MBean server registry */
    private MBeanServerRegistry registry = AgentInstance.getMBeanServerRegistry();

    /** MBean server name */
    private String mbsName;

    /** MBean name template (object name) */
    private String mbeanTemplate;

    /** Attribute name template */
    private String attrTemplate;

    /** Statistic name tmeplate */
    private String statisticTemplate;

    /** Execution time field */
    private String timeField;

    /** Timestamp field */
    private String tstamp;

    /** Cache mapping spy contexts to statistics */
    private Map<SpyContext,MethodCallStatistics> statsCache = new HashMap<SpyContext, MethodCallStatistics>();

    /**
     * Creates new method call statistics collector.
     *
     * @param mbsName mbean server name
     *
     * @param mbeanTemplate mbean name template (object name)
     *
     * @param attrTemplate attribute name template
     *
     * @param statisticTemplate statistic name template
     *
     * @param tstamp timestamp field name
     *
     * @param timeField execution time field name
     */
    public ZorkaStatsCollector(String mbsName, String mbeanTemplate, String attrTemplate,
                               String statisticTemplate, String tstamp, String timeField) {
        this.mbsName  = mbsName;
        this.mbeanTemplate = mbeanTemplate;
        this.attrTemplate = attrTemplate;
        this.statisticTemplate = statisticTemplate;
        this.timeField = timeField;
        this.tstamp = tstamp;
    }


    @Override
    public Map<String,Object> process(Map<String,Object> record) {

        if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
            log.debug("Collecting record: " + record);
        }

        SpyContext ctx = (SpyContext) record.get(".CTX");
        MethodCallStatistics stats = statsCache.get((SpyContext) record.get(".CTX"));

        if (stats == null) {
            stats = registry.getOrRegister(mbsName, ctx.subst(mbeanTemplate), ctx.subst(attrTemplate),
                    new MethodCallStatistics(), "Method call statistics");
        }

        String key = ObjectInspector.substitute(ctx.subst(statisticTemplate), record);

        MethodCallStatistic statistic = (MethodCallStatistic)stats.getMethodCallStatistic(key);

        Object timeObj = timeField != null ? record.get(timeField) : 0L;  // TODO WTF?
        Object tstampObj = tstamp != null ? record.get(tstamp) : 0L;   // TODO WTF?

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
                statistic.logError((Long)tstampObj, (Long)timeObj);
            } else {
                if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                    log.debug("No ON_RETURN nor ON_ERROR marked on record " + record);
                }
            }
        } else {
            if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
                log.debug("Unknown type of timeField or tstamp object: " + timeObj);
            }
        }

        return record;
    }

    /** Returns statistic template */
    public String getStatisticTemplate() {
        // TODO get rid of this method, use introspection in unit tests
        return statisticTemplate;
    }
}
