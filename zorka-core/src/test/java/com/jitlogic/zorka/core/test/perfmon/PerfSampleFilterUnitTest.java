/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.common.tracedata.Metric;
import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.RawDataMetric;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleMatcher;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PerfSampleFilterUnitTest extends ZorkaFixture {


    private PerfSampleMatcher pm(String pattern) {
        return new PerfSampleMatcher(symbols, pattern);
    }


    private void checkParse(String pm, boolean fa, String dom, String metric, String...attrs) {
        PerfSampleMatcher p = pm(pm);

        assertEquals("Bad freeAttrs: '" + pm + "'",
                fa, p.hasFreeAttrs());

        assertEquals("Bad domain: '" + pm + "'",
                dom, p.getDomainPattern().pattern());

        assertEquals("Bad metric name: '" + pm + "'",
                metric, p.getMetricPattern().pattern());

        Map<Integer, Pattern> atp = p.getAttrIdPatterns();
        assertEquals("Attribute count mismatch: '" + pm + "'",
                attrs.length/2, atp.size());

        for (int i = 1; i < attrs.length; i+=2) {
            String a = attrs[i-1], ap = attrs[i];
            int id = symbols.symbolId(a);

            assertTrue("Should have attribute '" + a + "' in: '" + pm + "' " + atp,
                    atp.containsKey(id));

            assertEquals("Attribute '" + a + "' should match '" + ap + "' in: '"
                            + pm + "' " + atp, ap, atp.get(id).pattern());
        }
    }


    @Test
    public void testParsePattern() {
        checkParse("zorka:type=ZorkaStats",
                false, "zorka", ".*",
                "type", "ZorkaStats");

        checkParse("zorka:type=ZorkaStats,*",
                true, "zorka", ".*",
                "type", "ZorkaStats");

        checkParse("*:type=ZorkaStats,*",
                true, ".*", ".*",
                "type", "ZorkaStats");

        checkParse("~z.*:name=~.*Http.*,calls",
                false, "z.*", "calls",
                "name", ".*Http.*");

        checkParse("z*:type=ZorkaStats,name=*Http*,tag=ALL,calls,*",
                true, "z.*", "calls",
                "name", ".*Http.*", "type", "ZorkaStats", "tag", "ALL");
    }


    private int lastId;

    private PerfSample sample(String name, Number value, String domain, String...attrs) {

        Metric m = new RawDataMetric(lastId++, name, "", domain,
                null); // TODO zamiast sample.attrs uzyc metric.attrs

        PerfSample sample = new PerfSample(m.getId(), value);
        sample.setMetric(m);

        Map<Integer,String> atm = new HashMap<Integer, String>();

        for (int i = 1; i < attrs.length; i+=2) {
            atm.put(symbols.symbolId(attrs[i-1]), attrs[i]);
        }

        sample.setAttrs(atm);

        return sample;
    }

    private void ckm(boolean match, PerfSample s, String pattern) {
        PerfSampleMatcher m = new PerfSampleMatcher(symbols, pattern);
        assertEquals("Sample: '" + s + "', pattern: '" + pattern, match, m.matches(s));
    }


    @Test
    public void testMatchSamples() {
        PerfSample s1 = sample("calls", 1.0, "zorka",
                "type", "ZorkaStats", "name", "HttpStats", "tag", "ALL");
        PerfSample s2 = sample("errors", 0.0, "zorka",
                "type", "ZorkaStats", "name", "HttpStats", "tag", "ALL");

        ckm(true,  s1, "zorka:type=ZorkaStats,calls,*");
        ckm(false, s2, "zorka:type=ZorkaStats,calls,*");

        ckm(false, s1, "zorka:type=ZorkaStats,calls");
        ckm(true,  s1, "zorka:type=ZorkaStats,name=HttpStats,tag=ALL,calls");

        ckm(true,  s1, "zorka:type=ZorkaStats,*");
        ckm(true,  s2, "zorka:type=ZorkaStats,*");
    }


    @Test
    public void testFilterSamples() {
        PerfSample s1 = sample("calls", 1.0, "zorka",
                "type", "ZorkaStats", "name", "HttpStats", "tag", "ALL");
        PerfSample s2 = sample("errors", 0.0, "zorka",
                "type", "ZorkaStats", "name", "HttpStats", "tag", "ALL");

        PerfSampleFilter f1 = new PerfSampleFilter(symbols,
                new HashMap<String, String>(),
                ZorkaUtil.<String, String>constMap("1", "zorka:type=ZorkaStats,calls,*"));
        assertEquals("Sample: '" + s1 + "', filter: '" + f1, false, f1.matches(s1));
        assertEquals("Sample: '" + s1 + "', filter: '" + f1, true, f1.matches(s2));

        PerfSampleFilter f2 = new PerfSampleFilter(symbols,
                ZorkaUtil.<String, String>constMap("1", "zorka:type=ZorkaStats,calls,*"),
                new HashMap<String, String>());
        assertEquals("Sample: '" + s1 + "', filter: '" + f2, true, f2.matches(s1));
        assertEquals("Sample: '" + s1 + "', filter: '" + f2, false, f2.matches(s2));
    }


    @Test
    public void testFilterAttrsExcl() {
        PerfAttrFilter f1 = new PerfAttrFilter(symbols,
                ZorkaUtil.<String, String>constMap("host", "test", "env", "PRD"),
                Collections.<String>emptyList(), Arrays.asList("xxx", "yyy"));
        for (Map.Entry<String,Boolean> e : ZorkaUtil.<String,Boolean>constMap(
                "aaa", true, "xxx", false, "host", false, "test", true, "env", false)
                .entrySet()) {
            assertEquals("Attribute: '" + e.getKey() + "', filter: " + f1,
                    e.getValue(), f1.matches(symbols.symbolId(e.getKey())));

        }
    }


    @Test
    public void testFilterAttrsIncl() {
        PerfAttrFilter f2 = new PerfAttrFilter(symbols,
                ZorkaUtil.<String, String>constMap("host", "test", "env", "PRD"),
                Arrays.asList("xxx", "yyy"), Collections.<String>emptyList());
        for (Map.Entry<String,Boolean> e : ZorkaUtil.<String,Boolean>constMap(
                "aaa", false, "xxx", true, "host", false, "test", false, "env", false)
                .entrySet()) {
            assertEquals("Attribute: '" + e.getKey() + "', filter: " + f2,
                    e.getValue(), f2.matches(symbols.symbolId(e.getKey())));

        }
    }
}
