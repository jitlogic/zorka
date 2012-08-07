package com.jitlogic.zorka.agent.testutil;

import com.jitlogic.zorka.agent.JavaAgent;
import com.jitlogic.zorka.agent.ZorkaBshAgent;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TestUtil {

    private MBeanServer mbs;
    private ZorkaBshAgent agent;


    public void setUp(ZorkaBshAgent agent) {
        mbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        this.agent = agent;
        this.agent.getMBeanServerRegistry().register("test", mbs);
    }


    public void tearDown() {
        agent.getMBeanServerRegistry().unregister("test");
    }


    public TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom); bean.setDiv(div);

        mbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

}
