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
package com.jitlogic.zorka.agent.testutil;

import com.jitlogic.zorka.agent.*;
import com.jitlogic.zorka.agent.testspy.support.TestCollectQueueProcessor;
import com.jitlogic.zorka.integ.snmp.SnmpLib;
import com.jitlogic.zorka.integ.syslog.SyslogLib;
import com.jitlogic.zorka.logproc.LogProcLib;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.util.ZorkaLogger;

import org.junit.After;
import org.junit.Before;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import java.util.Properties;

public class ZorkaFixture {

    protected Properties configProperties;
    protected TestLogger testLogger;
    protected MBeanServer testMbs;
    private ZorkaTestUtil testUtil;
    protected MBeanServerRegistry mBeanServerRegistry;

    protected AgentInstance agentInstance;
    protected SpyInstance spyInstance;

    protected SyslogLib syslogLib;
    protected SpyLib spy;
    protected SnmpLib snmpLib;

    protected ZorkaBshAgent zorkaAgent;
    protected ZorkaLib zorka;

    protected LogProcLib logproc;

    @Before
    public void setUpFixture() {
        configProperties = setProps(
                ZorkaConfig.defaultProperties(),
                "zorka.home.dir", "/tmp",
                "zabbix.enabled", "no",
                "zorka.hostname", "test",
                "spy", "yes"
                );

        ZorkaConfig.setProperties(configProperties);

        testLogger = new TestLogger();
        ZorkaLogger.setLogger(testLogger);

        configProperties = ZorkaConfig.getProperties();
        mBeanServerRegistry = new MBeanServerRegistry(true);
        AgentInstance.setMBeanServerRegistry(mBeanServerRegistry);

        agentInstance = new AgentInstance(configProperties, new TestExecutor());
        AgentInstance.setInstance(agentInstance);
        agentInstance.start();


        spyInstance = agentInstance.getSpyInstance();
        spyInstance.getSubmitter().setCollector(new TestCollectQueueProcessor());

        zorkaAgent = agentInstance.getZorkaAgent();
        zorka = zorkaAgent.getZorkaLib();

        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        mBeanServerRegistry.register("test", testMbs, testMbs.getClass().getClassLoader());

        testUtil = ZorkaTestUtil.setUp();

        syslogLib = agentInstance.getSyslogLib();
        snmpLib = agentInstance.getSnmpLib();
        spy = agentInstance.getSpyLib();

        logproc = agentInstance.getLogProcLib();
    }


    @After
    public void tearDownFixture() {
        AgentInstance.setMBeanServerRegistry(null);
        ZorkaLogger.setLogger(null);
        ZorkaConfig.cleanup();
    }


    public static Properties setProps(Properties props, String...data) {

        for (int i = 1; i < data.length; i+=2) {
            props.setProperty(data[i-1], data[i]);
        }

        return props;
    }
}
