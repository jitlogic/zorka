package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.bootstrap.AgentMain;
import com.jitlogic.zorka.util.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.Properties;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class BootstrapUtilsTest {

    private Properties props = new Properties();


    @Before
    public void setUp() {
        props.put("jboss.home", "/opt/jboss");
        props.put("zorka.classpath.jboss-system", "${jboss.home}/lib/jboss-system.jar,${jboss.home}/lib/jboss-management.jar");
        props.put("zorka.classpath.jboss-mx", "${jboss.home}/lib/jboss-mx.jar");
    }


    @Test
    public void testEvalPropStr() {
        assertEquals("/opt/jboss/lib/jboss-system.jar",
                ZorkaUtil.evalPropStr("${jboss.home}/lib/jboss-system.jar", props));
        assertEquals("path is /opt/jboss !",
                ZorkaUtil.evalPropStr("path is ${jboss.home} !", props));
        assertEquals("--${not.substituted}--",
                ZorkaUtil.evalPropStr("--${not.substituted}--", props));
    }


    @Test
    public void testEvalMultiPropStr() {
        assertEquals("/opt/jboss/lib/jboss-system.jar,/opt/jboss/lib/jboss-mx.jar",
                ZorkaUtil.evalPropStr("${jboss.home}/lib/jboss-system.jar,${jboss.home}/lib/jboss-mx.jar", props));
    }


    @Test
    public void testConstructExtClasspath() {
        URL[] urls = ZorkaConfig.getExtClasspath(props, false);

        assertEquals(3, urls.length);
    }

}
