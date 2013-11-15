package com.jitlogic.zorka.common.test.support;

import org.junit.Before;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;

public class CommonFixture {
    public MBeanServer testMbs;

    @Before
    public void setUpCommonFixture() throws Exception {
        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
    }

}