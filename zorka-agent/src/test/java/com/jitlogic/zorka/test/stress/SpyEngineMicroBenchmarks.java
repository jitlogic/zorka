/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.test.stress;

import com.jitlogic.zorka.test.stress.support.StressTestFixture;

import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyProcessor;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SpyEngineMicroBenchmarks extends StressTestFixture {

    @Test
    public void benchmarkInstrumentationWithNullProcessor() throws Exception {

        SpyDefinition sdef = SpyDefinition.instance()
            .onEnter(new SpyProcessor() {
                @Override
                public Map<String, Object> process(Map<String, Object> record) {
                    return record;
                }
            });

        runBenchmark("trivialMethod", sdef, System.out, "Null processor on method entry");
    }

    @Test
    public void benchmarkInstrumentationWithSingleSyncOperation() throws Exception {

        final AtomicLong counter = new AtomicLong(0);

        SpyDefinition sdef = SpyDefinition.instance()
                .onEnter(new SpyProcessor() {
                    @Override
                    public Map<String, Object> process(Map<String, Object> record) {
                        counter.incrementAndGet();
                        return record;
                    }
                });

        runBenchmark("trivialMethod", sdef, System.out, "Single atomic  operation on method entry");
    }

}
