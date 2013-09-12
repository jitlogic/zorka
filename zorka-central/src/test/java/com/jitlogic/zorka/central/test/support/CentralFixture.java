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

package com.jitlogic.zorka.central.test.support;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jitlogic.zorka.central.CentralConfig;
import com.jitlogic.zorka.central.HostStoreManager;
import com.jitlogic.zorka.central.rest.AdminService;
import com.jitlogic.zorka.central.rest.TraceDataService;
import com.jitlogic.zorka.common.test.support.TestUtil;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.zico.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Properties;

public class CentralFixture {

    private String tmpDir;
    private Properties configProperties;

    protected CentralConfig config;
    protected HostStoreManager storeManager;
    protected ZicoService zicoService;

    protected TraceDataService traceDataService;
    protected AdminService adminService;

    protected SymbolRegistry symbolRegistry;

    protected TestCentralModule testCentralModule;
    protected Injector injector;
    protected BasicDataSource dataSource;

    @Before
    public void setUpCentralFixture() throws Exception {
        tmpDir = "/tmp" + File.separatorChar + "zorka-unit-test";
        TestUtil.rmrf(tmpDir);
        new File(tmpDir).mkdirs();

        configProperties = setProps(
                ZorkaConfig.defaultProperties(CentralConfig.DEFAULT_CONF_PATH),
                "central.home.dir", tmpDir,
                "zico.service", "no",
                "central.db.type", "h2",
                "central.db.url", "jdbc:h2:mem:test",
                "central.db.user", "sa",
                "central.db.pass", "sa",
                "central.db.create", "yes"
        );

        config = new CentralConfig(configProperties);

        testCentralModule = new TestCentralModule(config);
        injector = Guice.createInjector(testCentralModule);

        dataSource = injector.getInstance(BasicDataSource.class);

        storeManager = injector.getInstance(HostStoreManager.class);
        zicoService = injector.getInstance(ZicoService.class);
        zicoService.start();

        traceDataService = injector.getInstance(TraceDataService.class);
        adminService = injector.getInstance(AdminService.class);

        symbolRegistry = injector.getInstance(SymbolRegistry.class);
    }

    @After
    public void tearDownCentralFixture() throws Exception {
        zicoService.stop();
        storeManager.close();
        dataSource.close();
    }

    public String getTmpDir() {
        return tmpDir;
    }


    public String tmpFile(String name) {
        return new File(getTmpDir(), name).getPath();
    }


    private static Properties setProps(Properties props, String... data) {

        for (int i = 1; i < data.length; i += 2) {
            props.setProperty(data[i - 1], data[i]);
        }

        return props;
    }

}
