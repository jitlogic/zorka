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

import com.jitlogic.zorka.central.CentralConfig;
import com.jitlogic.zorka.central.CentralInstance;
import com.jitlogic.zorka.central.StoreManager;
import com.jitlogic.zorka.common.test.support.TestUtil;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Properties;

public class CentralFixture {

    private String tmpDir;

    private Properties configProperties;

    protected CentralConfig config;

    protected CentralInstance instance;

    protected StoreManager storeManager;

    @Before
    public void setUpCentralFixture() throws Exception {
        tmpDir = "/tmp" + File.separatorChar + "zorka-unit-test";
        TestUtil.rmrf(tmpDir);
        new File(tmpDir).mkdirs();

        configProperties = setProps(
                ZorkaConfig.defaultProperties(CentralConfig.DEFAULT_CONF_PATH),
                "central.home.dir", tmpDir
        );

        config = new CentralConfig(configProperties);

        instance = new CentralInstance(config);
        instance.start();

        storeManager = instance.getStoreManager();
    }

    @After
    public void tearDownCentralFixture() throws Exception {
        instance.stop();
    }

    public String getTmpDir() {
        return tmpDir;
    }


    public String tmpFile(String name) {
        return new File(getTmpDir(), name).getPath();
    }


    private static Properties setProps(Properties props, String...data) {

        for (int i = 1; i < data.length; i+=2) {
            props.setProperty(data[i-1], data[i]);
        }

        return props;
    }

}
