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

package com.jitlogic.zico.test.support;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jitlogic.zico.core.TraceTypeRegistry;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.rest.AdminService;
import com.jitlogic.zico.core.rest.TraceDataService;
import com.jitlogic.zorka.common.test.support.TestUtil;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zico.core.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Properties;

public class ZicoFixture {

    private String tmpDir;
    private Properties configProperties;

    protected ZicoConfig config;
    protected HostStoreManager storeManager;
    protected ZicoService zicoService;

    protected TraceDataService traceDataService;
    protected AdminService adminService;

    protected SymbolRegistry symbolRegistry;
    protected TraceTypeRegistry traceTypeRegistry;

    protected TestZicoModule testZicoModule;
    protected Injector injector;
    protected BasicDataSource dataSource;

    @Before
    public void setUpZicoFixture() throws Exception {
        tmpDir = "/tmp" + File.separatorChar + "zorka-unit-test";
        TestUtil.rmrf(tmpDir);
        new File(tmpDir).mkdirs();

        configProperties = setProps(
                ZorkaConfig.defaultProperties(ZicoConfig.DEFAULT_CONF_PATH),
                "zico.home.dir", tmpDir,
                "zico.service", "no",
                "zico.db.type", "h2",
                "zico.db.url", "jdbc:h2:mem:test",
                "zico.db.user", "sa",
                "zico.db.pass", "sa",
                "zico.db.create", "yes"
        );

        config = new ZicoConfig(configProperties);

        testZicoModule = new TestZicoModule(config);
        injector = Guice.createInjector(testZicoModule);

        dataSource = injector.getInstance(BasicDataSource.class);

        storeManager = injector.getInstance(HostStoreManager.class);
        zicoService = injector.getInstance(ZicoService.class);
        zicoService.start();

        traceDataService = injector.getInstance(TraceDataService.class);
        adminService = injector.getInstance(AdminService.class);

        symbolRegistry = injector.getInstance(SymbolRegistry.class);
        traceTypeRegistry = injector.getInstance(TraceTypeRegistry.class);
    }

    @After
    public void tearDownZicoFixture() throws Exception {
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
