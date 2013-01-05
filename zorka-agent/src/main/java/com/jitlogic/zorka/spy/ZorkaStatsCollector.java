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

import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;

import java.util.Map;

import static com.jitlogic.zorka.spy.SpyLib.*;

/**
 * Maintains statistics about method calls and updates them using data from incoming records.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaStatsCollector extends AbstractStatsCollector implements SpyProcessor {



    /**
     * Creates new method call statistics collector.
     *
     * @param mbsName mbean server name
     *
     * @param mbeanTemplate mbean name template (object name)
     *
     * @param attrTemplate attribute name template
     *
     * @param statTemplate statistic name template
     *
     * @param tstamp timestamp field name
     *
     * @param timeField execution time field name
     */
    public ZorkaStatsCollector(String mbsName, String mbeanTemplate, String attrTemplate,
                               String statTemplate, String tstamp, String timeField) {

        super(mbsName, mbeanTemplate, attrTemplate, statTemplate, tstamp, timeField);

        if (mbeanFlags == 0 && attrFlags == 0) {

        }

    }


    @Override
    public Map<String,Object> process(Map<String,Object> record) {

        if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
            log.debug("Collecting record: " + record);
        }

        SpyContext ctx = (SpyContext) record.get(".CTX");

        prefetch(record, ctx);

        MethodCallStatistics stats = statsCacheEnabled ? statsCache.get(ctx) : null;

        if (stats == null) {
            String mbeanName = subst(mbeanTemplate, record, ctx, mbeanFlags);
            String attrName = subst(attrTemplate, record, ctx, attrFlags);
            stats = registry.getOrRegister(mbsName, mbeanName, attrName, new MethodCallStatistics(), "Call stats");
            if (statsCacheEnabled) {
                statsCache.putIfAbsent(ctx, stats);
            }
        }

        String key = statFlags != 0 ? subst(statTemplate, record,  ctx,  statFlags) : statTemplate;

        MethodCallStatistic stat = (MethodCallStatistic) stats.getMethodCallStatistic(key);

        submit(record, stat);

        return record;
    }


}
