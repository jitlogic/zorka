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

package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jitlogic.zorka.api.SpyLib.SPD_ARGPROC;

/**
 * Filters records using regular expressions.
 */
public class RegexFilterProcessor implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private String src, dst;
    private Pattern regex;
    private String expr = null, defval = null;
    private Boolean filterOut;


    public RegexFilterProcessor(String src, String regex) {
        this(src, regex, false);
    }


    public RegexFilterProcessor(String src, String regex, Boolean filterOut) {
        this.src = src;
        this.regex = Pattern.compile(regex);
        this.filterOut = filterOut;
    }


    public RegexFilterProcessor(String src, String dst, String regex, String expr, Boolean filterOut) {
        this(src, regex, filterOut);
        this.dst = dst;
        this.expr = expr;
    }


    public RegexFilterProcessor(String src, String dst, String regex, String expr, String defval) {
        this(src, dst, regex, expr, (Boolean)null);
        this.defval = defval;
    }


    public SpyRecord process(SpyRecord record) {
        Object val = record.get(src);

        if (expr == null) {
            boolean matches = val != null && regex.matcher(val.toString()).matches();

            if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
                log.debug("Filtering '" + val + "' through '" + regex.pattern() + "': " + matches);
            }

            return matches^filterOut ? record : null;
        } else if (val != null) {
            Matcher matcher = regex.matcher(val.toString());
            if (matcher.matches()) {
                Object[] vals = new Object[matcher.groupCount()+1];
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = matcher.group(i);
                }
                String subst = ObjectInspector.substitute(expr, vals);
                record.put(dst, subst);
                if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
                    log.debug("Processed '" + val + "' to '" + subst + "' using pattern '" + regex.pattern() + "'");
                }
            } else if (defval != null) {
                record.put(dst, defval);
                if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
                    log.debug("No value to be processed. Using default value of '" + defval + "'");
                }
            } else if (Boolean.TRUE.equals(filterOut)) {
                if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
                    log.debug("No value to be processed. Filtering out.");
                }
                return null;
            }
        } else {
            if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
                log.debug("No value to be processed. Leaving record unprocessed.");
            }
        }
        return record;
    }
}
