/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.stats;

/**
 *
 */
public class MethodSlaStatistic implements ZorkaStat {

    private long threshold;

    private long calls;

    private long errors;

    public MethodSlaStatistic(int threshold) {
        this.threshold = threshold * 1000000L;
    }

    public synchronized double getSla() {
        return calls != 0 ? 100.0 * (calls - errors) / calls : 100.0;
    }

    public synchronized double getSlaCLR() {
        double rslt = getSla();
        calls = 0; errors = 0;
        return rslt;
    }

    public synchronized void logCall(long t) {
        calls++;

        if (t > threshold) {
            errors++;
        }
    }

    public synchronized  void logError(long t) {
        calls++;
        errors++;
    }

    @Override
    public String getName() {
        return ""+threshold;
    }

    @Override
    public String getUnit() {
        return "ms";
    }

    @Override
    public String getDescription() {
        return "SLA metric for " + threshold + "ms threshold.";
    }

    @Override
    public String toString() {
        return "SLA(sla=" + String.format("%.2d", getSla()) + ")";
    }
}
