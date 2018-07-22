package com.jitlogic.zorka.common.test.support;

import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.util.ObjectInspector;
import org.junit.Before;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;

public class CommonFixture {
    public MBeanServer testMbs;

    protected TestHttpClient httpClient;

    @Before
    public void setUpCommonFixture() throws Exception {
        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        httpClient = new TestHttpClient();
        ObjectInspector.setField(HttpUtil.class, "client", httpClient);
    }

}