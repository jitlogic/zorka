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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.integ;


import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * This class implements common parts of nagios commands. It is responsible for assembling
 * and formatting result.
 *
 */
public abstract class AbstractNagiosCommand implements NagiosCommand {

    /** No RC calculation - always return OK */
    public static final int RC_NONE = 0;

    /** MIN RC calculation - alert when calculated utilization goes below threshold. */
    public static final int RC_MIN = 1;

    /** MAX RC calculation - alert when calculated utilization goes over threshold. */
    public static final int RC_MAX = 2;

    protected static final String[] RC_CODES = { "OK", "WARNING", "CRITICAL", "UNKNOWN" };


    /** Sum of all components in summary */
    public static final int SEL_SUM = 0;

    /** Choose first result as summary */
    public static final int SEL_FIRST = 1;

    /** Choose record by name as summary */
    public static final int SEL_ONE = 2;


    /** Determines how result code should be calculated. See RC_* constants for details */
    protected int rcMode;

    /** Attribute used to calculate status, eg. DVAL0, LSUM1 etc. */
    protected String rcAttr;

    /** Warning threshold - crossing it will result in WARNING status. */
    protected double rcWarn;

    /** Alert threshold - crossing it will result in CRITICAL status. */
    protected double rcAlrt;


    /** Summary selection mode (see SEL_* constants) */
    protected int selMode;

    /** When choosing selected result as summary: name of attribute by which result will be chosen. */
    protected String selName;

    /** When choosing selected result as summary: value of attribute by which result will be chosen. */
    protected Set<String> selVals = new HashSet<String>();

    protected String tmplSummary = "";

    /** Label */
    protected String tmplLabel = "";

    /** Perf Line */
    protected String tmplPerfLine = "";

    /** Text Line */
    protected String tmplTextLine = "";


    /**
     * Sets minimum alert thresholds.
     *
     * @param rcWarn warning level (WARN)
     *
     * @param rcAlrt alert level (CRITICAL)
     */
    public AbstractNagiosCommand withRcMin(String rcAttr, double rcWarn, double rcAlrt) {
        this.rcAttr = rcAttr;
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MIN;
        return this;
    }


    /**
     * Sets maximum alert thresholds.
     *
     * @param rcWarn warning level (WARN)
     *
     * @param rcAlrt alert level (CRITICAL)
     */
    public AbstractNagiosCommand withRcMax(String rcAttr, double rcWarn, double rcAlrt) {
        this.rcAttr = rcAttr;
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MAX;
        return this;
    }


    /**
     * Calculates sums of all results as summary.
     */
    public AbstractNagiosCommand withSumResult(String selName) {
        this.selMode = SEL_SUM;
        this.selName = selName;
        return this;
    }


    /**
     * Chooses first result as summary.
     */
    public AbstractNagiosCommand withFirstResult() {
        this.selMode = SEL_FIRST;
        return this;
    }


    /**
     * Selects specific result as summary (one of selected values)
     *
     * @param selName attribute name to be checked
     *
     * @param selVals desired attribute values
     */
    public AbstractNagiosCommand withFilterResult(String selName, String...selVals) {
        this.selMode = SEL_ONE;
        this.selName = selName;
        this.selVals = ZorkaUtil.set(selVals);
        return this;
    }



    public AbstractNagiosCommand withLabel(String tmplLabel) {
        this.tmplLabel = tmplLabel;
        return this;
    }



    public AbstractNagiosCommand withPerfLine(String tmplPerfLine) {
        this.tmplPerfLine = tmplPerfLine;
        return this;
    }



    public AbstractNagiosCommand withTextLine(String tmplTextLine) {
        this.tmplTextLine = tmplTextLine;
        return this;
    }

    /**
     *
     * Sets template for summary test line.
     *
     * @param tmplSummary - summary template
     */
    public AbstractNagiosCommand withSummaryLine(String tmplSummary) {
        this.tmplSummary = tmplSummary;
        return this;
    }

}
