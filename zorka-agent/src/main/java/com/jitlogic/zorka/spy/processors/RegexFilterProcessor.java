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

import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters records using regular expressions.
 */
public class RegexFilterProcessor implements SpyProcessor {

    private int src, dst;
    private Pattern regex;
    private String expr = null, defval = null;
    private Boolean filterOut;

    ObjectInspector inspector;


    public RegexFilterProcessor(int src, String regex) {
        this(src, regex, false);
    }


    public RegexFilterProcessor(int src, String regex, Boolean filterOut) {
        this.src = src;
        this.regex = Pattern.compile(regex);
        this.filterOut = filterOut;
    }


    public RegexFilterProcessor(int src, int dst, String regex, String expr, Boolean filterOut) {
        this(src, regex, filterOut);
        this.dst = dst;
        this.expr = expr;
        inspector = new ObjectInspector();
    }


    public RegexFilterProcessor(int src, int dst, String regex, String expr, String defval) {
        this(src, dst, regex, expr, (Boolean)null);
        this.defval = defval;
    }


    public SpyRecord process(int stage, SpyRecord record) {
        Object val = record.get(stage, src);

        if (expr == null) {
            boolean matches = val != null && regex.matcher(val.toString()).matches();
            return matches^filterOut ? record : null;
        } else if (val != null) {
            Matcher matcher = regex.matcher(val.toString());
            if (matcher.matches()) {
                Object[] vals = new Object[matcher.groupCount()+1];
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = matcher.group(i);
                }
                record.put(stage, dst, inspector.substitute(expr, vals));
            } else if (defval != null) {
                record.put(stage, dst, defval);
            } else if (Boolean.TRUE.equals(filterOut)) {
                return null;
            }
        }
        return record;
    }
}
