package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class PerfSampleMatcher {

    private final static Pattern RE_ANY = Pattern.compile(".*");
    private final static Pattern RE_ATTRVAL = Pattern.compile("([\\w\\d]+)=(.+)");
    private final static Pattern RE_PATTERN = Pattern.compile("([^:]+):(.*)");

    private final static Set<Character> CTL_CHARS = ZorkaUtil.constSet(
            '.', '*', '+', '?', '\\', '[', ']', '(', ')', '^', '$', '{', '}', ':', '|', '>');

    private boolean freeAttrs = false;
    private Pattern domainPattern, metricPattern = RE_ANY;
    private Map<Integer,Pattern> attrIdPatterns;
    private Map<String,Pattern> attrPatterns;


    public PerfSampleMatcher(SymbolRegistry registry, String pattern) {
        Matcher m1 = RE_PATTERN.matcher(pattern);
        if (!m1.matches()) {
            throw new ZorkaRuntimeException("Cannot parse pattern '" + pattern + "': bad syntax");
        }

        domainPattern = mkRegex(m1.group(1));

        attrIdPatterns = new HashMap<Integer, Pattern>();
        attrPatterns = new HashMap<String, Pattern>();

        for (String seg : m1.group(2).split(",")) {
            if ("*".equals(seg)) {
                // Free attributes marker
                freeAttrs = true;
            } else {
                Matcher m2 = RE_ATTRVAL.matcher(seg);
                if (m2.matches()) {
                    // Attribute-value pair
                    String k = m2.group(1);
                    Pattern p = mkRegex(m2.group(2));
                    attrIdPatterns.put(registry.symbolId(k), p);
                    attrPatterns.put(k, p);
                } else {
                    // Metric name
                    metricPattern = mkRegex(seg);
                }
            }
        }

    }


    private Pattern mkRegex(String s) {
        if (s.startsWith("~")) {
            return Pattern.compile(s.substring(1));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ('*' == c) {
                    sb.append('.');
                } else if (CTL_CHARS.contains(c)) {
                    sb.append('\\');
                }
                sb.append(c);
            }
            return Pattern.compile(sb.toString());
        }
    }


    public boolean matches(PerfSample sample) {

        String domain = sample.getMetric().getDomain();

        // Check domain
        if (domain != null && !domainPattern.matcher(domain).matches()) {
            return false;
        }

        String metric = sample.getMetric().getName();

        if (metric != null && !metricPattern.matcher(metric).matches()) {
            return false;
        }

        Map<Integer,String> sa = sample.getAttrs();

        int asize = 0;
        if (sa != null) {
            asize += sa.size();
            for (Map.Entry<Integer, Pattern> e : attrIdPatterns.entrySet()) {
                String v = sa.get(e.getKey());
                if (v == null || !e.getValue().matcher(v).matches()) {
                    return false;
                }
            }
        }

        Map<String,Object> ma = sample.getMetric().getAttrs();
        if (ma != null) {
            asize += ma.size();
            for (Map.Entry<String, Pattern> e : attrPatterns.entrySet()) {
                Object v = ma.get(e.getKey());
                if (v == null || !e.getValue().matcher(v.toString()).matches()) {
                    return false;
                }
            }
        }

        return freeAttrs || asize == attrIdPatterns.size();
    }

    public Pattern getDomainPattern() {
        return domainPattern;
    }

    public Pattern getMetricPattern() {
        return metricPattern;
    }

    public Map<Integer,Pattern> getAttrIdPatterns() {
        return attrIdPatterns;
    }

    public boolean hasFreeAttrs() {
        return freeAttrs;
    }
}
