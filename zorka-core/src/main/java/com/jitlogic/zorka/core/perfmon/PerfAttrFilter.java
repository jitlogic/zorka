/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PerfAttrFilter {

    private Set<Integer> includeIds = new HashSet<Integer>();
    private Set<Integer> excludeIds = new HashSet<Integer>();
    private Set<String> includeAttrs = new HashSet<String>();
    private Set<String> excludeAttrs = new HashSet<String>();

    public PerfAttrFilter(SymbolRegistry symbolRegistry,
                          Map<String,String> constAttrMap,
                          List<String> includeAttrs,
                          List<String> excludeAttrs) {

        if (includeAttrs != null && includeAttrs.size() > 0) {
            this.includeAttrs.addAll(includeAttrs);
            for (String attr : includeAttrs) {
                this.includeIds.add(symbolRegistry.symbolId(attr));
            }
        }

        if (excludeAttrs != null && excludeAttrs.size() > 0) {
            this.excludeAttrs.addAll(excludeAttrs);
            this.excludeIds = new HashSet<Integer>();
            for (String attr : excludeAttrs) {
                this.excludeIds.add(symbolRegistry.symbolId(attr));
            }
        }

        if (constAttrMap != null && constAttrMap.size() > 0) {
            for (Map.Entry<String,String> e : constAttrMap.entrySet()) {
                this.excludeIds.add(symbolRegistry.symbolId(e.getKey()));
            }
        }

    }


    public boolean matches(int attrId) {
        // TODO implement regex/mask matches here
        if (includeIds.size() > 0) {
            return includeIds.contains(attrId);
        } else {
            return !excludeIds.contains(attrId);
        }
    }

    public boolean matches(String attr) {
        if (includeAttrs.size() > 0) {
            return includeAttrs.contains(attr);
        } else {
            return !excludeAttrs.contains(attr);
        }
    }


}
