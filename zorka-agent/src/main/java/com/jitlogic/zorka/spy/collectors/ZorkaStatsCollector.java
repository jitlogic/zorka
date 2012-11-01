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

import com.jitlogic.zorka.agent.AgentGlobals;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.spy.InstrumentationContext;
import com.jitlogic.zorka.spy.SpyRecord;

import java.util.HashMap;
import java.util.Map;

import static com.jitlogic.zorka.spy.SpyConst.*;

public class ZorkaStatsCollector implements SpyCollector {

    private MBeanServerRegistry registry = AgentGlobals.getMBeanServerRegistry();
    private String mbsName, mbeanTemplate, attrTemplate, keyTemplate;
    private int timeField;

    private Map<InstrumentationContext,MethodCallStatistics> statsCache =
            new HashMap<InstrumentationContext, MethodCallStatistics>();


    public ZorkaStatsCollector(String mbsName, String mbeanTemplate, String attrTemplate,
                               String keyTemplate, int timeField) {
        this.mbsName  = mbsName;
        this.mbeanTemplate = mbeanTemplate;
        this.attrTemplate = attrTemplate;
        this.keyTemplate = keyTemplate;
        this.timeField = timeField;
    }


    public void collect(SpyRecord record) {

        InstrumentationContext ctx = record.getContext();
        MethodCallStatistics stats = statsCache.get(record.getContext());

        if (stats == null) {
            stats = registry.getOrRegisterBeanAttr(mbsName, subst(mbeanTemplate, ctx), subst(attrTemplate, ctx),
                        new MethodCallStatistics(), "Method call statistics");
        }

        String key = subst(keyTemplate, ctx);

        MethodCallStatistic statistic = (MethodCallStatistic)stats.getMethodCallStatistic(key);

        Object timeObj = record.get(ON_COLLECT, timeField);

        if (timeObj instanceof Long) {
            if (record.gotStage(ON_EXIT)) {
                statistic.logCall(0, (Long)record.get(ON_COLLECT, timeField));
            } else if (record.gotStage(ON_ERROR)) {
                statistic.logError(0, (Long)record.get(ON_COLLECT, timeField));
            } // else (log error [warning] here)
        } // else (log error [warning] here)
    }


    private String subst(String template, InstrumentationContext ctx) {
        return template
                .replace("${className}", ctx.getClassName())
                .replace("${methodName}", ctx.getMethodName())
                .replace("${shortClassName}", ctx.getShortClassName());
    }


}
