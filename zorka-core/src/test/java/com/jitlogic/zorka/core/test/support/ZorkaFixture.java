/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.http.HttpHandler;
import com.jitlogic.zorka.common.test.support.CommonFixture;
import com.jitlogic.zorka.common.test.support.TestJmx;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.*;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.integ.SyslogLib;
import com.jitlogic.zorka.core.integ.zabbix.ZabbixLib;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.PerfMonLib;
import com.jitlogic.zorka.core.spy.*;

import com.jitlogic.zorka.common.test.support.CommonTestUtil;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.SpyClassLookup;
import com.jitlogic.zorka.core.spy.ltracer.LTracer;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.test.spy.support.TestSpyRetransformer;
import org.junit.After;
import org.junit.Before;

import javax.management.ObjectName;
import java.io.File;
import java.util.Properties;

import static org.junit.Assert.*;

public class ZorkaFixture extends CommonFixture {

    protected Properties configProperties;
    protected MBeanServerRegistry mBeanServerRegistry;

    protected AgentInstance agentInstance;
    protected SpyClassTransformer spyTransformer;
    protected TestSpyRetransformer spyRetransformer;

    protected TestTaskScheduler taskScheduler;

    protected SyslogLib syslogLib;
    protected SpyLib spy;
    protected TracerLib tracer;

    protected ZorkaBshAgent zorkaAgent;
    protected ZorkaLib zorka;

    protected PerfMonLib perfmon;
    protected AgentConfig config;

    protected SymbolRegistry symbols;

    protected ZabbixLib zabbixLib;
    protected UtilLib util;

    protected QueryTranslator translator;

    protected HttpHandler httpClient;

    protected String tmpDir;

    public ZorkaFixture() {
        configProperties = CommonTestUtil.setProps(
                ZorkaConfig.defaultProperties(AgentConfig.DEFAULT_CONF_PATH),
                "zorka.home.dir", "/tmp",
                "zabbix.enabled", "no",
                "zorka.hostname", "test",
                "zorka.log.file", "no",
                "zorka.mbs.autoregister", "yes",
                "scripts", "",
                "spy", "yes",
                "scripts.auto", "yes",
                "auto.com.jitlogic.zorka.core.test.spy.probe", "test.bsh"
        );
    }

    @Before
    public void setUpFixture() throws Exception {

        // Configure and spawn agent instance ...

        AgentConfig.persistent = false;


        taskScheduler = TestTaskScheduler.instance();

        config = new AgentConfig(configProperties);
        spyRetransformer = new TestSpyRetransformer();
        agentInstance = new AgentInstance(config, spyRetransformer);
        agentInstance.start();

        // Get all agent components used by tests

        mBeanServerRegistry = agentInstance.getMBeanServerRegistry();
        zorkaAgent = agentInstance.getZorkaAgent();
        zorka = agentInstance.getZorkaLib();
        syslogLib = agentInstance.getSyslogLib();
        spy = agentInstance.getSpyLib();
        tracer = agentInstance.getTracerLib();
        perfmon = agentInstance.getPerfMonLib();
        spyTransformer = agentInstance.getClassTransformer();
        zabbixLib = agentInstance.getZabbixLib();
        translator = agentInstance.getTranslator();
        util = agentInstance.getUtilLib();

        // Install test MBean server

        mBeanServerRegistry.register("test", testMbs, testMbs.getClass().getClassLoader());

        MainSubmitter.setSubmitter(agentInstance.getSubmitter());
        Tracer tracer = agentInstance.getTracer();
        if (tracer instanceof LTracer) {
            MainSubmitter.setTracer(tracer);
        } else {
            MainSubmitter.setTracer(tracer);
        }

        symbols = agentInstance.getSymbolRegistry();

        tmpDir = "/tmp" + File.separatorChar + "zorka-unit-test";
        ZorkaUtil.rmrf(tmpDir);
        assertTrue(new File(tmpDir).mkdirs());

        httpClient = new TestHttpClient();

        SpyClassLookup.INSTANCE = new SpyClassLookup();
    }


    @After
    public void tearDownFixture() throws Exception {

        // Uninstall test MBean server
        mBeanServerRegistry.unregister("test");

        MainSubmitter.setTracer(null);
        MainSubmitter.setSubmitter(null);

        TraceHandler.setMinMethodTime(TraceHandler.DEFAULT_MIN_METHOD_TIME);
        TraceHandler.setTuningEnabled(false);
        TraceHandler.setTuningLongThreshold(TraceHandler.TUNING_DEFAULT_LCALL_THRESHOLD);
        TraceHandler.setTuningDefaultExchInterval(TraceHandler.TUNING_DEFAULT_EXCH_INTERVAL);

        TraceHandler.setTuningExchangeMinCalls(TraceHandler.TUNING_EXCHANGE_CALLS_DEFV);
    }


    public TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom);
        bean.setDiv(div);

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

    public int sid(String symbol) {
        return symbols.symbolId(symbol);
    }

    public TraceRecord tr(String className, String methodName, String methodSignature,
                          long calls, long errors, int flags, long time,
                          TraceRecord... children) {

        TraceRecord tr = new TraceRecord(null);
        tr.setClassId(sid(className));
        tr.setMethodId(sid(methodName));
        tr.setSignatureId(sid(methodSignature));
        tr.setCalls(calls);
        tr.setErrors(errors);
        tr.setFlags(flags);
        tr.setTstop(time);

        for (TraceRecord child : children) {
            child.setParent(tr);
            tr.addChild(child);
        }

        return tr;
    }


}
