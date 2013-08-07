/** 
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.agent;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.jitlogic.zorka.core.*;
import com.jitlogic.zorka.core.test.support.TestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.util.ObjectDumper;
import com.jitlogic.zorka.core.integ.ZabbixLib;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class BshAgentUnitTest extends ZorkaFixture {

    @Before
    public void setUp() throws Exception {
        mBeanServerRegistry.register("java", java.lang.management.ManagementFactory.getPlatformMBeanServer(), null);
        ZabbixLib zl = new ZabbixLib(mBeanServerRegistry, config);
        zorkaAgent.put("zabbix", zl);
        zorkaAgent.loadScript(getClass().getResource("/unittest/BshAgentTest.bsh").getPath());
    }


	@Test
	public void testTrivialQuery() throws Exception {
		assertEquals("5", zorkaAgent.query("2+3"));
	}


	@Test
	public void testJmxCalls() throws Exception {
		assertTrue("1.0", zorkaAgent.query("zorka.jmx(\"java\",\"java.lang:type=Runtime\",\"SpecVersion\")").startsWith("1."));
	}


	//@Test @Ignore("Broken, to be fixed.")
	public void testCreateMappedMBeanWithNoAttrs() throws Exception {
		assertEquals("ZorkaMappedMBean()", zorkaAgent.query("zorka.mbean(\"java\", \"zorka:type=jvm,name=GCstats\")"));
		
		String rslt = zorkaAgent.query("zorka.jmx(\"java\", \"zorka:type=jvm,name=GCstats\")");
		assertEquals("zorka:type=jvm,name=GCstats", rslt);
	}


	//@Test @Ignore("Broken, to be fixed.")
	public void testCreateMappedMBeanWithConstantAttr() throws Exception {
		assertEquals("OK", zorkaAgent.query("createBean1()"));
		String rslt = zorkaAgent.query("zorka.jmx(\"java\", \"zorka.test:name=Bean1\", \"test1\")");
		assertEquals("1", rslt);
	}


    @Test
    public void testZabbixDiscoveryFunc() throws Exception {
        Map<String,List<Map<String,String>>> obj = (Map<String,List<Map<String,String>>>)
                zorkaAgent.eval("zabbix._discovery(\"java\", \"java.lang:type=MemoryPool,*\", \"name\", \"type\")");
        assertTrue("should return map", obj != null);
         List<Map<String,String>> data = obj.get("data");
        assertTrue("obj.data should be non-empty", data.size() > 0);
    }


    @Test
    public void testNewBshEllipsis() throws Exception {
        zorkaAgent.put("test", new SomeTestLib());
        String rslt = zorkaAgent.query("test.join(\"a\", \"b\", \"c\")");
        assertEquals("a:bc", rslt);
    }


    @Test
    public void testStartAndLoadProfilesAndScripts() throws Exception {
        URL url = getClass().getResource("/cfgp");
        AgentConfig config = new AgentConfig(url.getPath());
        TestUtil.setField(zorkaAgent, "config", config);
        zorkaAgent.loadScripts();
        assertNotNull("common.bsh script should be loaded.", zorkaAgent.get("common_bsh"));
        assertNotNull("jvm/jvm.bsh script should be loaded.", zorkaAgent.get("jvm_bsh"));
        assertTrue("property values for jvm module should be present", config.hasCfg("jvm.test.property"));
        assertFalse("profile.scripts properties should be filtered off", config.hasCfg("profile.scripts"));
        assertNotNull("test/test.bsh script should be loaded.", zorkaAgent.get("test_bsh"));
        assertEquals("jvm/jvm.bsh script should be called only once.", "bar", zorkaAgent.get("not_to_be_overridden"));
    }


    @Test
    public void testObjectDumper() throws Exception {
        Object obj = query("zorka.jmx(\"java\",\"java.lang:type=Runtime\")");
        String s = ObjectDumper.objectDump(obj);
        assertTrue(s != null && s.length() > 100);
    }


    private Object query(final String src) throws Exception {
        ZorkaBasicCallback callback = new ZorkaBasicCallback();
        ZorkaBshWorker worker = new ZorkaBshWorker(Executors.newSingleThreadExecutor(), 5000,
                agentInstance.getZorkaAgent(), src, callback);
        worker.run();
        return callback.getResult();
    }

    public static class SomeTestLib {
        public String join(String foo, String...parts) {
            StringBuilder sb = new StringBuilder();
            sb.append(foo); sb.append(":");
            for (String part : parts)
                sb.append(part);
            return sb.toString();
        }
    }


}
