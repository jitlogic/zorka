/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.rankproc;

import java.util.Map;
import java.util.Set;

public class RawDataMetric extends Metric {


    public RawDataMetric(MetricTemplate template, Set<Map.Entry<String, Object>> attrSet) {
        super(template, attrSet);
    }


    public Number getValue(long clock, QueryResult result) {
        Double multiplier = getTemplate().getMultiplier();
        if (multiplier != null) {
            Number rslt = (Number)result.getValue();
            if (rslt instanceof Double || Math.floor(multiplier) != multiplier) {
                return multiplier * rslt.doubleValue();
            } else {
                return multiplier.longValue() * rslt.longValue();
            }
        } else {
            return (Number)result.getValue();
        }
    }

}
