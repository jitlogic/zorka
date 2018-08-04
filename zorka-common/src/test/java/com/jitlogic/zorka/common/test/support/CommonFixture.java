/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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