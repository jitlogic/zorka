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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class PerfSampleFilter {
    private List<PerfSampleMatcher> includes;
    private List<PerfSampleMatcher> excludes;

    public PerfSampleFilter(SymbolRegistry registry,
                            Map<String, String> includeMap,
                            Map<String,String> excludeMap) {

        this.includes = parseMatchers(registry, includeMap);
        this.excludes = parseMatchers(registry, excludeMap);

    }

    private List<PerfSampleMatcher> parseMatchers(SymbolRegistry registry, Map<String,String> matchers) {
        Map<String,PerfSampleMatcher> pmap = new TreeMap<String, PerfSampleMatcher>();

        for (Map.Entry<String,String> e : matchers.entrySet()) {
            pmap.put(e.getKey(), new PerfSampleMatcher(registry, e.getValue()));
        }

        List<PerfSampleMatcher> rslt = new ArrayList<PerfSampleMatcher>(pmap.size()+1);

        for (Map.Entry<String,PerfSampleMatcher> e : pmap.entrySet()) {
            rslt.add(e.getValue());
        }

        return rslt;
    }


    public boolean matches(PerfSample sample) {

        boolean rslt;

        if (includes.size() == 0) {
            rslt = true;
        } else {
            rslt = false;
            for (PerfSampleMatcher pm : includes) {
                if (pm.matches(sample)) {
                    rslt = true;
                    break;
                }
            }
        }

        if (!rslt) {
            return false;
        }

        for (PerfSampleMatcher pm : excludes) {
            if (pm.matches(sample)) {
                return false;
            }
        }

        return true;
    }

}
