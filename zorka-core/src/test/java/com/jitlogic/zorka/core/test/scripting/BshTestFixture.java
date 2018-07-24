package com.jitlogic.zorka.core.test.scripting;

import com.jitlogic.zorka.common.test.support.CommonTestUtil;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.AgentConfig;
import com.jitlogic.zorka.core.AgentInstance;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.spy.DummySpyRetransformer;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

class BshTestFixture {

    private Properties configProperties = CommonTestUtil.setProps(
            ZorkaConfig.defaultProperties(AgentConfig.DEFAULT_CONF_PATH),
            "zorka.home.dir", "/tmp",
            "zabbix.enabled", "no",
            "zorka.hostname", "test",
            "zorka.filelog", "no",
            "zorka.mbs.autoregister", "yes",
            "scripts", "",
            "spy", "yes",
            "scripts.auto", "yes",
            "auto.com.jitlogic.zorka.core.test.spy.probe", "test.bsh"
    );


    AgentInstance instance(String...props) {
        AgentConfig config = new AgentConfig(configProperties);

        for (int i = 1; i < props.length; i += 2) {
            config.setCfg(props[i-1], props[i]);
        }

        MBeanServer testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);

        AgentInstance inst = new AgentInstance(config, new DummySpyRetransformer(null, config));

        inst.getMBeanServerRegistry().register("test", testMbs, testMbs.getClass().getClassLoader());
        ZorkaBshAgent za = inst.getZorkaAgent();

        za.put("zorka", inst.getZorkaLib());
        za.put("spy", inst.getSpyLib());
        za.put("tracer", inst.getTracerLib());
        za.put("util", inst.getUtilLib());
        za.put("syslog", inst.getSyslogLib());

        return inst;
    }

    AgentInstance checkLoadScript(String script, String...props) {
        AgentInstance inst = instance(props);
        assertEquals("OK", inst.getZorkaAgent().loadScript(script));
        return inst;
    }

    Map<String,Object> rec(Object...args) {
        Map<String,Object> r = new HashMap<String, Object>();

        for (int i = 1; i < args.length; i+=2) {
            r.put(args[i-1].toString(), args[i]);
        }

        return r;
    }
}
