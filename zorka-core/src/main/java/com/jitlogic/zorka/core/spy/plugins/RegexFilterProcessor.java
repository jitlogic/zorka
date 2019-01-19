/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters records using regular expressions.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class RegexFilterProcessor implements SpyProcessor {

    /**
     * Logger
     */
    private Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Source field
     */
    private String src;

    /**
     * Destination field
     */
    private String dst;

    /**
     * Regular expression
     */
    private Pattern regex;

    /**
     * Substitution expression
     */
    private String expr;

    /**
     * Default value
     */
    private String defval;

    /**
     * Inverse filter flag
     */
    private Boolean filterOut;

    /**
     * Transforms of additional arguments
     */
    private Map<String,String> transforms;

    /**
     * Creates regex filter that filters records based on supplied regular expression.
     *
     * @param src   source field
     * @param regex regular expression
     */
    public RegexFilterProcessor(String src, String regex) {
        this(src, regex, false);
    }

    /**
     * Creates regex filter that filters records based on supplied regular expression.
     *
     * @param src       source field
     * @param regex     filter regular expression
     * @param filterOut inverse filtering if true
     */
    public RegexFilterProcessor(String src, String regex, Boolean filterOut) {
        this.src = src;
        this.regex = Pattern.compile(regex);
        this.filterOut = filterOut;
    }


    /**
     * Creates regex filter that transforms records based on supplied regular expression.
     *
     * @param src       source field
     * @param dst       destination field
     * @param regex     filter regular expression
     * @param expr      substitution regular expression (with ${n} marking matches from filter expression)
     * @param filterOut inverse filtering if true
     */
    public RegexFilterProcessor(String src, String dst, String regex, String expr, Boolean filterOut) {
        this(src, regex, filterOut);
        this.dst = dst;
        this.expr = expr;
    }


    /**
     * Creates regex filter that transforms records based on supplied regular expression.
     *
     * @param src    source field
     * @param dst    destination field
     * @param regex  filter regular expression
     * @param expr   substitution regular expression (with ${n} marking matches from filter expression)
     * @param defval default value
     */
    public RegexFilterProcessor(String src, String dst, String regex, String expr, String defval) {
        this(src, dst, regex, expr, (Boolean) null);
        this.defval = defval;
    }

    public RegexFilterProcessor transform(String dst, String expr) {
        if (transforms == null) {
            transforms = new HashMap<String,String>();
        }
        transforms.put(dst, expr);
        return this;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object val = record.get(src);

        if (expr == null) {
            boolean matches = val != null && regex.matcher(val.toString()).matches();

            if (log.isDebugEnabled()) {
                log.debug("Filtering '" + val + "' through '" + regex.pattern() + "': " + matches);
            }

            return matches ^ filterOut ? record : null;
        } else if (val != null) {
            Matcher matcher = regex.matcher(val.toString());
            if (matcher.matches()) {
                Object[] vals = new Object[matcher.groupCount() + 1];
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = matcher.group(i);
                }
                String subst = ObjectInspector.substitute(expr, vals);
                record.put(dst, subst);
                if (transforms != null) {
                    for (Map.Entry<String,String> e : transforms.entrySet()) {
                        record.put(e.getKey(), ObjectInspector.substitute(e.getValue(), vals));
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Processed '" + val + "' to '" + subst + "' using pattern '" + regex.pattern() + "'");
                }
            } else if (defval != null) {
                record.put(dst, defval);
                if (log.isDebugEnabled()) {
                    log.debug("No value to be processed. Using default value of '" + defval + "'");
                }
            } else if (Boolean.TRUE.equals(filterOut)) {
                if (log.isDebugEnabled()) {
                    log.debug("No value to be processed. Filtering out.");
                }
                return null;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No value to be processed. Leaving record unprocessed.");
            }
        }
        return record;
    }
}
