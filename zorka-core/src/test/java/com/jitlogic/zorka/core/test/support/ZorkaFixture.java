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
package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.*;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.integ.SnmpLib;
import com.jitlogic.zorka.core.integ.SyslogLib;
import com.jitlogic.zorka.core.integ.ZabbixLib;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.PerfMonLib;
import com.jitlogic.zorka.core.spy.MainSubmitter;
import com.jitlogic.zorka.core.spy.SpyClassTransformer;
import com.jitlogic.zorka.core.spy.SpyLib;
import com.jitlogic.zorka.core.spy.TracerLib;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.junit.After;
import org.junit.Before;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import java.io.File;
import java.util.Properties;

public class ZorkaFixture {

    protected Properties configProperties;
    protected MBeanServer testMbs;
    protected MBeanServerRegistry mBeanServerRegistry;

    protected AgentInstance agentInstance;
    protected SpyClassTransformer spyTransformer;

    protected SyslogLib syslogLib;
    protected SpyLib spy;
    protected TracerLib tracer;
    protected SnmpLib snmpLib;

    protected ZorkaBshAgent zorkaAgent;
    protected ZorkaLib zorka;

    protected PerfMonLib perfmon;
    protected AgentConfig config;

    protected SymbolRegistry symbols;

    protected ZabbixLib zabbixLib;

    protected QueryTranslator translator;

    private String tmpDir;

    @Before
    public void setUpFixture() throws Exception {

        // Configure and spawn agent instance ...

        configProperties = setProps(
                ZorkaConfig.defaultProperties(AgentConfig.DEFAULT_CONF_PATH),
                "zorka.home.dir", "/tmp",
                "zabbix.enabled", "no",
                "zorka.hostname", "test",
                "zorka.filelog", "no",
                "zorka.mbs.autoregister", "yes",
                "spy", "yes"
                );

        config = new AgentConfig(configProperties);
        agentInstance = new AgentInstance(config); //AgentInstance.instance();
        agentInstance.start();

        // Get all agent components used by tests

        mBeanServerRegistry = agentInstance.getMBeanServerRegistry();
        zorkaAgent = agentInstance.getZorkaAgent();
        zorka = zorkaAgent.getZorkaLib();
        syslogLib = agentInstance.getSyslogLib();
        snmpLib = agentInstance.getSnmpLib();
        spy = agentInstance.getSpyLib();
        tracer = agentInstance.getTracerLib();
        perfmon = agentInstance.getPerfMonLib();
        spyTransformer = agentInstance.getClassTransformer();
        zabbixLib = agentInstance.getZabbixLib();
        translator = agentInstance.getTranslator();

        // Install test MBean server

        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        mBeanServerRegistry.register("test", testMbs, testMbs.getClass().getClassLoader());

        MainSubmitter.setSubmitter(agentInstance.getSubmitter());
        MainSubmitter.setTracer(agentInstance.getTracer());

        symbols = agentInstance.getSymbolRegistry();

        tmpDir = "/tmp" + File.separatorChar + "zorka-unit-test";
        TestUtil.rmrf(tmpDir);
        new File(tmpDir).mkdirs();
    }


    @After
    public void tearDownFixture() throws Exception {

        // Uninstall test MBean server
        mBeanServerRegistry.unregister("test");

        // Stop agent
        agentInstance.stop();

        MainSubmitter.setSubmitter(null);
        MainSubmitter.setTracer(null);
    }

    public String getTmpDir() {
        return tmpDir;
    }

    private static Properties setProps(Properties props, String...data) {

        for (int i = 1; i < data.length; i+=2) {
            props.setProperty(data[i-1], data[i]);
        }

        return props;
    }
}
