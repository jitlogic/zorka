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
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

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
    private String statTemplate;

    /** Execution time field */
    private String timeField;

    /** Timestamp field */
    private String tstamp;

    /** String substitution flags */
    private int mbeanFlags;

    private int attrFlags;

    private int statFlags;

    private int prefetch;

    protected static final String CLASS_NAME = "className";
    protected static final String METHOD_NAME = "methodName";
    protected static final String CLASS_SNAME = "shortClassName";
    protected static final String PACKAGE_NAME = "packageName";

    protected static final String M_CLASS_NAME = "${className}";
    protected static final String M_METHOD_NAME = "${methodName}";
    protected static final String M_CLASS_SNAME = "${shortClassName}";
    protected static final String M_PACKAGE_NAME = "${packageName}";

    protected static final int HAS_CLASS_NAME       = 0x01;
    protected static final int HAS_METHOD_NAME      = 0x02;
    protected static final int HAS_CLASS_SNAME      = 0x04;
    protected static final int HAS_PACKAGE_NAME     = 0x08;
    protected static final int HAS_OTHER_NAME       = 0x10;
    protected static final int SINGLE_MACRO         = 0x20;

    /** Cache mapping spy contexts to statistics */
    private ConcurrentHashMap<SpyContext,MethodCallStatistics> statsCache
            = new ConcurrentHashMap<SpyContext, MethodCallStatistics>();

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
        this.mbsName  = mbsName;
        this.mbeanTemplate = mbeanTemplate.intern();
        this.attrTemplate = attrTemplate.intern();
        this.statTemplate = statTemplate.intern();
        this.timeField = timeField;
        this.tstamp = tstamp;

        this.mbeanFlags = templateFlags(mbeanTemplate);
        this.attrFlags = templateFlags(attrTemplate);
        this.statFlags = templateFlags(statTemplate);

        if (needsCtxAttr(HAS_CLASS_NAME)) {
            prefetch |= HAS_CLASS_NAME;
        }

        if (needsCtxAttr(HAS_METHOD_NAME)) {
            prefetch |= HAS_METHOD_NAME;
        }

        if (needsCtxAttr(HAS_CLASS_SNAME)) {
            prefetch |= HAS_CLASS_SNAME;
        }

        if (needsCtxAttr(HAS_PACKAGE_NAME)) {
            prefetch |= HAS_PACKAGE_NAME;
        }

    }


    @Override
    public Map<String,Object> process(Map<String,Object> record) {

        if (SpyInstance.isDebugEnabled(SPD_COLLECTORS)) {
            log.debug("Collecting record: " + record);
        }

        SpyContext ctx = (SpyContext) record.get(".CTX");

        if (0 !=  (prefetch & HAS_CLASS_NAME)) {
            record.put(CLASS_NAME, ctx.getClassName());
        }

        if (0 !=  (prefetch & HAS_METHOD_NAME)) {
            record.put(METHOD_NAME, ctx.getMethodName());
        }

        if (0 !=  (prefetch & HAS_CLASS_SNAME)) {
            record.put(CLASS_SNAME, ctx.getShortClassName());
        }

        if (0 !=  (prefetch & HAS_PACKAGE_NAME)) {
            record.put(PACKAGE_NAME, ctx.getPackageName());
        }

        boolean cacheStats = !(0 != ((mbeanFlags|attrFlags) & HAS_OTHER_NAME));

        MethodCallStatistics stats = cacheStats ? statsCache.get(ctx) : null;

        if (stats == null) {
            stats =
                registry.getOrRegister(mbsName,
                        subst(mbeanTemplate, record, ctx, mbeanFlags),
                        subst(attrTemplate, record, ctx, attrFlags),
                        new MethodCallStatistics(), "Method call statistics");
            if (cacheStats) {
                statsCache.putIfAbsent(ctx, stats);
            }
        }

        String key = statFlags != 0 ? subst(statTemplate, record,  ctx,  statFlags) : statTemplate;

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


    protected String subst(String input, Map<String,Object> record, SpyContext ctx, int flags) {

        if (flags == 0) {
            return input;
        }

        if (0 != (flags & SINGLE_MACRO)) {
            switch (flags & (~SINGLE_MACRO)) {
                case HAS_CLASS_NAME:
                    return ctx.getClassName();
                case HAS_METHOD_NAME:
                    return ctx.getMethodName();
                case HAS_CLASS_SNAME:
                    return ctx.getShortClassName();
                case HAS_PACKAGE_NAME:
                    return ctx.getPackageName();
                default:
                    // TODO internal error - should be logged somewhere ...
                    break;
            }
            return input;
        } else {
            return ObjectInspector.substitute(input, record);
        }
    }


    protected boolean needsCtxAttr(int ref) {
        for (int flag : new int[] { mbeanFlags, attrFlags, statFlags }) {
            if (0 == (flag & HAS_OTHER_NAME) && 0 != (flag & ref)) {
                return true;
            }
        }

        return false;
    }


    protected int templateFlags(String input) {
        int rv = 0;

        Matcher m = ObjectInspector.reVarSubstPattern.matcher(input);

        while (m.find()) {
            String s = m.group(1);
            if (CLASS_NAME.equals(s)) {
                rv |= HAS_CLASS_NAME;
            } else if (METHOD_NAME.equals(s)) {
                rv |= HAS_METHOD_NAME;
            } else if (CLASS_SNAME.equals(s)) {
                rv |= HAS_CLASS_SNAME;
            } else if (PACKAGE_NAME.equals(s)) {
                rv |= HAS_PACKAGE_NAME;
            } else {
                rv |= HAS_OTHER_NAME;
            }
        }

        if (M_CLASS_NAME.equals(input) || M_METHOD_NAME.equals(input) ||
            M_CLASS_SNAME.equals(input) || M_PACKAGE_NAME.equals(input)) {
            rv |= SINGLE_MACRO;
        }

        return rv;
    }


    /** Returns statistic template */
    public String getStatTemplate() {
        // TODO get rid of this method, use introspection in unit tests
        return statTemplate;
    }
}
