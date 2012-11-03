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

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.MainSubmitter;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static com.jitlogic.zorka.agent.testspy.BytecodeInstrumentationUnitTest.*;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import java.util.Properties;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.*;

public class SpyInstanceIntegTest {

    private MBeanServerRegistry registry;
    private MBeanServer testMbs;

    private SpyInstance instance;

    @Before
    public void setUp() {
        registry = new MBeanServerRegistry(true);
        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        registry.register("test", testMbs, null);
        AgentInstance.setMBeanServerRegistry(registry);

        instance = new SpyInstance(new Properties());
        MainSubmitter.setSubmitter(instance.getSubmitter());
    }


    public void tearDown() {
        MainSubmitter.setSubmitter(null);
        AgentInstance.setMBeanServerRegistry(null);
    }

    @Test
    public void testTrivialMethodRun() throws Exception {
        SpyDefinition sdef = SpyDefinition.instrument().onSubmit().timeDiff(0, 1, 1)
                .lookFor(TCLASS1, "trivialMethod")
                .toStats("test", "test:name=${shortClassName}", "stats", "${methodName}", 0, 1);

        instance.add(sdef);

        Object obj = instantiate(instance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        Object stats = getAttr("test", "test:name=TestClass1", "stats");
        assertNotNull(stats);
    }


}
