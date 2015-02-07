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
import java.util.List;

public class PerfRecord implements SymbolicRecord {

    private long clock;
    private int scannerId;
    private List<PerfSample> samples;


    public PerfRecord(long clock, int scannerId, List<PerfSample> samples) {
        this.clock = clock;
        this.scannerId = scannerId;
        this.samples = samples;
    }


    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        scannerId = checker.checkSymbol(scannerId, this);
        for (PerfSample sample : samples) {
            checker.checkMetric(sample.getMetricId());
        }
    }

    public long getClock() {
        return clock;
    }

    public int getScannerId() {
        return scannerId;
    }

    public List<PerfSample> getSamples() {
        return samples;
    }
}
