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

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.spy.SpyDefinition;

import org.junit.Test;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.core.test.support.TestUtil.*;

public class SpyInstanceIntegTest extends ZorkaFixture {

    @Test
    public void testTrivialMethodRun() throws Exception {
        SpyDefinition sdef = spy.instrument("x")
                .include(spy.byMethod(BytecodeInstrumentationFixture.TCLASS1, "trivialMethod"))
                .onSubmit(spy.zorkaStats("test", "test:name=${shortClassName}", "stats", "${methodName}"));

        agentInstance.getClassTransformer().add(sdef);

        Object obj = instantiate(agentInstance.getClassTransformer(), BytecodeInstrumentationFixture.TCLASS1);
        invoke(obj, "trivialMethod");

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=TestClass1", "stats");
        assertNotNull(stats);
        assertEquals(1, stats.getMethodCallStatistic("trivialMethod").getCalls());
    }


}
