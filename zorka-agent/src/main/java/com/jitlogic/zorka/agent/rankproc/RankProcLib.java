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

import com.jitlogic.zorka.common.MetricTemplate;

public class RankProcLib {


    public MetricTemplate rawDataMetric(String name, String units) {
        return new MetricTemplate(MetricTemplate.RAW_DATA, name, units);
    }


    public MetricTemplate timedDeltaMetric(String name, String units) {
        return new MetricTemplate(MetricTemplate.TIMED_DELTA, name, units);
    }


    public MetricTemplate rawDeltaMetric(String name, String units) {
        return new MetricTemplate(MetricTemplate.RAW_DELTA, name, units);
    }


    public MetricTemplate windowedRateMetric(String name, String units, String nom, String div) {
        return new MetricTemplate(MetricTemplate.WINDOWED_RATE, name, units, nom, div);
    }

}
