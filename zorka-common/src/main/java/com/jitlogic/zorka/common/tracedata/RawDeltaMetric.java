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

package com.jitlogic.zorka.common.tracedata;

import java.util.Map;
import java.util.Set;

/**
 * Raw delta metric. Calculates raw delta from submitted data.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class RawDeltaMetric extends RawDataMetric {

    /**
     * Value from last measurement
     */
    private Number last;

    public RawDeltaMetric(int id, String name, String description, Map<String, Object> attrs) {
        super(id, name, description, attrs);
    }

    public RawDeltaMetric(int id, int templateId, String name, String description, Map<String, Object> attrs) {
        super(id, templateId, name, description, attrs);
    }

    public RawDeltaMetric(MetricTemplate template, String name, String description, Map<String, Object> attrs) {
        super(template, name, description, attrs);
    }

    @Override
    public Number getValue(long clock, Object value) {
        Number cur = super.getValue(clock, value), rslt;

        if (cur instanceof Double || cur instanceof Float || last instanceof Double) {
            rslt = last != null ? cur.doubleValue() - last.doubleValue() : 0.0;
            last = cur.doubleValue();
        } else {
            rslt = last != null ? cur.longValue() - last.longValue() : 0L;
            last = cur.longValue();
        }

        return rslt;
    }
}
