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
package com.jitlogic.zorka.agent.test.support;

import com.jitlogic.zorka.agent.*;
import com.jitlogic.zorka.agent.integ.SnmpLib;
import com.jitlogic.zorka.agent.integ.SyslogLib;
import com.jitlogic.zorka.agent.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.agent.perfmon.PerfMonLib;
import com.jitlogic.zorka.agent.spy.SpyClassTransformer;
import com.jitlogic.zorka.agent.spy.SpyInstance;
import com.jitlogic.zorka.agent.spy.SpyLib;
import com.jitlogic.zorka.agent.spy.TracerLib;
import com.jitlogic.zorka.common.ZorkaLogger;

import org.junit.After;
import org.junit.Before;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import java.util.Properties;

public class ZorkaFixture {

    protected Properties configProperties;
    protected MBeanServer testMbs;
    protected MBeanServerRegistry mBeanServerRegistry;

    protected AgentInstance agentInstance;
    protected SpyInstance spyInstance;
    protected SpyClassTransformer spyTransformer;

    protected SyslogLib syslogLib;
    protected SpyLib spy;
    protected TracerLib tracer;
    protected SnmpLib snmpLib;

    protected ZorkaBshAgent zorkaAgent;
    protected ZorkaLib zorka;

    protected PerfMonLib perfmon;

    @Before
    public void setUpFixture() throws Exception {

        configProperties = setProps(
                ZorkaConfig.defaultProperties(),
                "zorka.home.dir", "/tmp",
                "zabbix.enabled", "no",
                "zorka.hostname", "test",
                "zorka.filelog", "no",
                "zorka.mbs.autoregister", "yes",
                "spy", "yes"
                );

        ZorkaConfig.setProperties(configProperties);

        configProperties = ZorkaConfig.getProperties();

        agentInstance = new AgentInstance(configProperties);
        mBeanServerRegistry = agentInstance.getMBeanServerRegistry();
        TestUtil.setField(agentInstance, "instance", agentInstance);
        agentInstance.start();

        spyInstance = agentInstance.getSpyInstance();

        zorkaAgent = agentInstance.getZorkaAgent();
        zorka = zorkaAgent.getZorkaLib();

        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        mBeanServerRegistry.register("test", testMbs, testMbs.getClass().getClassLoader());

        syslogLib = agentInstance.getSyslogLib();
        snmpLib = agentInstance.getSnmpLib();
        spy = agentInstance.getSpyLib();
        tracer = agentInstance.getTracerLib();
        perfmon = agentInstance.getPerfMonLib();

        spyTransformer = spyInstance.getClassTransformer();
    }


    @After
    public void tearDownFixture() {
        ZorkaLogger.setLogger(null);
        ZorkaConfig.cleanup();
        mBeanServerRegistry.unregister("test");
    }


    public static Properties setProps(Properties props, String...data) {

        for (int i = 1; i < data.length; i+=2) {
            props.setProperty(data[i-1], data[i]);
        }

        return props;
    }
}
