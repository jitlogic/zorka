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

import java.io.IOException;

/**
 * Objects implementing this interface can be passed to traverse() method
 * of all data objects (trace records, performance metrics etc.). For all
 * symbols found in trace data checkSymbol() method of passed object will
 * be invoked. For all metrics found in trace data checkMetric() will be
 * invoked.
 */
public interface MetadataChecker {

    /**
     * Metod called for every symbol found in trace data.
     */
    int checkSymbol(int symbolId, Object owner) throws IOException;

    /**
     * Method called for every metric found in trace data.
     */
    void checkMetric(int metricId) throws IOException;
}
