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
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.ZicoService;
import com.jitlogic.zico.core.services.HostGwtService;
import com.jitlogic.zico.core.services.SystemGwtService;
import com.jitlogic.zico.core.services.TraceDataGwtService;
import com.jitlogic.zico.core.services.UserGwtService;
import com.jitlogic.zorka.common.test.support.TestUtil;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Properties;
import java.util.Random;

public class ZicoFixture {

    private String tmpDir;
    private Properties configProperties;

    protected ZicoConfig config;
    protected HostStoreManager hostStoreManager;
    protected ZicoService zicoService;

    protected TraceDataGwtService traceDataService;
    protected SystemGwtService systemService;
    protected HostGwtService hostService;
    protected UserGwtService userService;

    protected SymbolRegistry symbolRegistry;

    protected TestZicoModule testZicoModule;
    protected Injector injector;

    protected UserTestContext userContext;

    protected Random rand = new Random();

    @Before
    public void setUpZicoFixture() throws Exception {
        tmpDir = "/tmp" + File.separatorChar + "zorka-unit-test";
        TestUtil.rmrf(tmpDir);
        new File(tmpDir).mkdirs();

        configProperties = ZicoTestUtil.setProps(
                ZorkaConfig.defaultProperties(ZicoConfig.DEFAULT_CONF_PATH),
                "zico.home.dir", tmpDir,
                "zico.listen.port", "9640",
                "zico.service", "no",
                "zico.db.type", "h2",
                "zico.db.url", "jdbc:h2:mem:test",
                "zico.db.user", "sa",
                "zico.db.pass", "sa",
                "zico.db.create", "yes",
                "dbwriter.synchronous.mode", "yes"
        );

        config = new ZicoConfig(configProperties);

        testZicoModule = new TestZicoModule(config);
        injector = Guice.createInjector(testZicoModule);

        hostStoreManager = injector.getInstance(HostStoreManager.class);
        zicoService = injector.getInstance(ZicoService.class);
        zicoService.start();

        traceDataService = injector.getInstance(TraceDataGwtService.class);
        systemService = injector.getInstance(SystemGwtService.class);
        hostService = injector.getInstance(HostGwtService.class);
        userService = injector.getInstance(UserGwtService.class);

        symbolRegistry = injector.getInstance(SymbolRegistry.class);

        userContext = (UserTestContext)injector.getInstance(UserContext.class);

    }

    @After
    public void tearDownZicoFixture() throws Exception {
        zicoService.stop();
        hostStoreManager.close();
    }

    public String getTmpDir() {
        return tmpDir;
    }


    public String tmpFile(String name) {
        return new File(getTmpDir(), name).getPath();
    }

    public byte[] rand(int size) {
        byte[] data = new byte[size];
        rand.nextBytes(data);
        return data;
    }



}
