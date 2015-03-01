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
import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.core.spy.SpyContext;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static com.jitlogic.zorka.core.spy.SpyLib.*;

/**
 * Maintains statistics about method calls and updates them using data from incoming records.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaStatsCollector implements SpyProcessor {

    public static final int ACTION_STATS = 0x01;
    public static final int ACTION_ENTER = 0x02;
    public static final int ACTION_EXIT = 0x04;

    public static final String CLASS_NAME = "className";
    public static final String METHOD_NAME = "methodName";
    public static final String CLASS_SNAME = "shortClassName";
    public static final String PACKAGE_NAME = "packageName";

    protected static final String M_CLASS_NAME = "${className}";
    protected static final String M_METHOD_NAME = "${methodName}";
    protected static final String M_CLASS_SNAME = "${shortClassName}";
    protected static final String M_PACKAGE_NAME = "${packageName}";

    protected static final int HAS_CLASS_NAME = 0x01;
    protected static final int HAS_METHOD_NAME = 0x02;
    protected static final int HAS_CLASS_SNAME = 0x04;
    protected static final int HAS_PACKAGE_NAME = 0x08;
    protected static final int HAS_OTHER_NAME = 0x10;
    protected static final int HAS_SINGLE_MACRO = 0x20;

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Actions that will be taken when method execution hits collector
     */
    private int actions;

    /**
     * MBean server name
     */
    private String mbsName;

    /**
     * MBean name template (object name)
     */
    private String mbeanTemplate;

    /**
     * Attribute name template
     */
    private String attrTemplate;

    /**
     * Statistic name tmeplate
     */
    private String statTemplate;

    /**
     * Execution time field
     */
    private String timeField;

    /**
     * Throughput field
     */
    private String throughputField;

    /**
     * Object Name substitution flags
     */
    private int mbeanFlags;

    /**
     * MBean Attribute substitution flags
     */
    private int attrFlags;

    /**
     * Statistic name substitution flags
     */
    private int statFlags;

    /**
     * Context attributes prefetch flags (marks context attributes that will be added to processed records)
     */
    private int prefetchFlags;

    /**
     * Cache mapping spy contexts to statistics
     */
    private ConcurrentHashMap<SpyContext, MethodCallStatistics> statsCache
            = new ConcurrentHashMap<SpyContext, MethodCallStatistics>();

    /**
     * This flag determines whether statsCache is actually usable for us
     */
    private boolean statsCacheEnabled;

    private MethodCallStatistic cachedStatistic;

    private MethodCallStatistics cachedStatistics;

    /**
     * MBean server registry
     */
    private MBeanServerRegistry registry;


    /**
     * Creates new method call statistics collector.
     *
     * @param mbsName       mbean server name
     * @param mbeanTemplate mbean name template (object name)
     * @param attrTemplate  attribute name template
     * @param statTemplate  statistic name template
     * @param timeField     execution time field name
     */
    public ZorkaStatsCollector(MBeanServerRegistry mbsRegistry, String mbsName, String mbeanTemplate,
                               String attrTemplate, String statTemplate, String timeField, String throughputField,
                               int actions) {

        // Some strings are intern()ed immediately, so

        this.registry = mbsRegistry;
        this.mbsName = mbsName;
        this.mbeanTemplate = mbeanTemplate.intern();
        this.attrTemplate = attrTemplate.intern();
        this.statTemplate = statTemplate.intern();

        this.timeField = timeField;
        this.throughputField = throughputField;
        this.actions = actions;

        this.mbeanFlags = templateFlags(mbeanTemplate);
        this.attrFlags = templateFlags(attrTemplate);
        this.statFlags = templateFlags(statTemplate);

        if (needsCtxAttr(HAS_CLASS_NAME)) {
            prefetchFlags |= HAS_CLASS_NAME;
        }

        if (needsCtxAttr(HAS_METHOD_NAME)) {
            prefetchFlags |= HAS_METHOD_NAME;
        }

        if (needsCtxAttr(HAS_CLASS_SNAME)) {
            prefetchFlags |= HAS_CLASS_SNAME;
        }

        if (needsCtxAttr(HAS_PACKAGE_NAME)) {
            prefetchFlags |= HAS_PACKAGE_NAME;
        }

        statsCacheEnabled = !(0 != ((mbeanFlags | attrFlags) & HAS_OTHER_NAME));

        if (mbeanFlags == 0 && attrFlags == 0) {
            // Object name and attribute name are constant ...
            cachedStatistics = registry.getOrRegister(mbsName, mbeanTemplate, attrTemplate,
                    new MethodCallStatistics(), "Call stats");

            if (statFlags == 0) {
                cachedStatistic = cachedStatistics.getMethodCallStatistic(statTemplate);
            }
        }

    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
            log.debug(ZorkaLogger.ZSP_ARGPROC, "Collecting record: " + record);
        }

        MethodCallStatistic statistic = cachedStatistic;

        if (statistic == null) {

            MethodCallStatistics statistics = cachedStatistics;
            SpyContext ctx = (SpyContext) record.get(".CTX");

            if (statistics == null) {
                prefetch(record, ctx);

                statistics = statsCacheEnabled ? statsCache.get(ctx) : null;

                if (statistics == null) {
                    String mbeanName = subst(mbeanTemplate, record, ctx, mbeanFlags);
                    String attrName = subst(attrTemplate, record, ctx, attrFlags);
                    statistics = registry.getOrRegister(mbsName, mbeanName, attrName,
                            new MethodCallStatistics(), "Call stats");
                    if (statsCacheEnabled) {
                        statsCache.putIfAbsent(ctx, statistics);
                    }
                }
            }

            String key = statFlags != 0 ? subst(statTemplate, record, ctx, statFlags) : statTemplate;

            statistic = statistics.getMethodCallStatistic(key);
        }

        if (0 != (actions & ACTION_STATS)) {
            submit(record, statistic);
        }

        if (0 != (actions & ACTION_ENTER)) {
            statistic.markEnter();
        }

        if (0 != (actions & ACTION_EXIT)) {
            statistic.markExit();
        }

        return record;
    }


    /**
     * Returns true if given context attribute is needed to format at least one string.
     * Strings that consist solely of context attribute macro are not counted.
     *
     * @param attr attribute bit (see HAS_* constants)
     * @return true if used (thus should be added to spy record)
     */
    private boolean needsCtxAttr(int attr) {
        for (int flags : new int[]{mbeanFlags, attrFlags, statFlags}) {
            if (0 == (flags & HAS_OTHER_NAME) && 0 != (flags & attr)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Determines what kinds of attributes are used in template string
     *
     * @param input template string
     * @return integer flags (see HAS_* constants)
     */
    private int templateFlags(String input) {
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
            rv |= HAS_SINGLE_MACRO;
        }

        return rv;
    }


    /**
     * Fetches required spy context attributes (method name, class name etc.)
     * and stores them in spy record.
     *
     * @param record spy record
     * @param ctx    spy context
     */
    private void prefetch(Map<String, Object> record, SpyContext ctx) {
        if (0 != (prefetchFlags & HAS_CLASS_NAME)) {
            record.put(CLASS_NAME, ctx.getClassName());
        }

        if (0 != (prefetchFlags & HAS_METHOD_NAME)) {
            record.put(METHOD_NAME, ctx.getMethodName());
        }

        if (0 != (prefetchFlags & HAS_CLASS_SNAME)) {
            record.put(CLASS_SNAME, ctx.getShortClassName());
        }

        if (0 != (prefetchFlags & HAS_PACKAGE_NAME)) {
            record.put(PACKAGE_NAME, ctx.getPackageName());
        }
    }


    /**
     * Performs string substitution. Chooses the fastest possible way to do so.
     *
     * @param input  template string
     * @param record spy record (with attributes used to do substitution)
     * @param ctx    spy context
     * @param flags  template flags.
     * @return
     */
    private String subst(String input, Map<String, Object> record, SpyContext ctx, int flags) {

        if (flags == 0) {
            return input;
        }

        if (0 != (flags & HAS_SINGLE_MACRO)) {
            switch (flags & (~HAS_SINGLE_MACRO)) {
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


    /**
     * Submits value to method call statistics.
     *
     * @param record    spy record
     * @param statistic statistic used to
     */
    private void submit(Map<String, Object> record, MethodCallStatistic statistic) {
        Object executionTime = record.get(timeField);
        Number throughput = null;

        if (throughputField != null) {
            Object v = record.get(throughputField);
            if (v instanceof Number) {
                throughput = (Number) v;
            } else {
                if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                    log.debug(ZorkaLogger.ZSP_ARGPROC, "Invalid value of throughput field: " + throughput);
                }
            }
        }

        if (executionTime instanceof Long) {
            if (0 != ((Integer) record.get(".STAGES") & (1 << ON_RETURN))) {
                if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                    log.debug(ZorkaLogger.ZSP_ARGPROC, "Updating stats using logCall()");
                }
                if (throughput != null) {
                    statistic.logCall((Long) executionTime, throughput.longValue());
                } else {
                    statistic.logCall((Long) executionTime);
                }
            } else if (0 != ((Integer) record.get(".STAGES") & (1 << ON_ERROR))) {
                if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                    log.debug(ZorkaLogger.ZSP_ARGPROC, "Updating stats using logError()");
                }
                if (throughput != null) {
                    statistic.logError((Long) executionTime, throughput.longValue());
                } else {
                    statistic.logError((Long) executionTime);
                }
            } else {
                if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                    log.debug(ZorkaLogger.ZSP_ARGPROC, "No ON_RETURN nor ON_ERROR marked on record " + record);
                }
            }
        } else {
            if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                log.debug(ZorkaLogger.ZSP_ARGPROC, "Unknown type of timeField: " + executionTime);
            }
        }
    }
}
