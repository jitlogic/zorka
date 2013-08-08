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
package com.jitlogic.zorka.central.test;


import com.jitlogic.zorka.central.Store;
import com.jitlogic.zorka.central.test.support.CentralFixture;

import com.jitlogic.zorka.common.test.support.TestTraceGenerator;
import com.jitlogic.zorka.common.tracedata.FressianTraceWriter;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoService;
import com.jitlogic.zorka.common.zico.ZicoTraceOutput;

import org.junit.Test;
import static org.junit.Assert.*;

import static org.fest.reflect.core.Reflection.*;

public class DataCollectionIntegTest extends CentralFixture {

    @Test
    public void testCollectSingleTraceRecord() throws Exception {
        // Start collector service ...
        ZicoService svc = instance.getZicoService();
        svc.start();

        TestTraceGenerator tg = new TestTraceGenerator();
        FressianTraceWriter ftw = new FressianTraceWriter(tg.getSymbols(), tg.getMetrics());
        ZicoTraceOutput output = new ZicoTraceOutput(ftw, "127.0.0.1", 8640, "test", "aaa");
        TraceRecord rec = tg.generate();

        method("open").in(output).invoke();
        output.submit(rec);
        method("runCycle").in(output).invoke();

        Store store = storeManager.get("test");
        assertEquals(1, store.getTraces().size());
    }

}
