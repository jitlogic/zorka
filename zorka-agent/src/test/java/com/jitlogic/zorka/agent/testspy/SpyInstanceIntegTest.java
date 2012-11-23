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

package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.spy.SpyDefinition;

import org.junit.Test;
import static org.junit.Assert.*;

import static com.jitlogic.zorka.agent.testspy.BytecodeInstrumentationUnitTest.*;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.*;

public class SpyInstanceIntegTest extends ZorkaFixture {

    @Test
    public void testTrivialMethodRun() throws Exception {
        SpyDefinition sdef = SpyDefinition.instrument().onSubmit().timeDiff(0, 1, 1)
                .include(TCLASS1, "trivialMethod")
                .toStats("test", "test:name=${shortClassName}", "stats", "${methodName}", 0, 1);

        spyInstance.add(sdef);

        Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        Object stats = getAttr("test", "test:name=TestClass1", "stats");
        assertNotNull(stats);
    }


}
