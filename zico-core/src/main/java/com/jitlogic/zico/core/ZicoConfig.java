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
package com.jitlogic.zico.core;


import com.jitlogic.zico.core.rds.RAGZOutputStream;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import javax.inject.Singleton;
import java.util.Properties;

@Singleton
public class ZicoConfig extends ZorkaConfig {

    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zico/zico.properties";

    public ZicoConfig(Properties props) {
        properties = props;
        homeDir = props.getProperty("zico.home.dir");
        setBaseProps();
    }

    public ZicoConfig(String home) {
        loadProperties(home, "zico.properties", DEFAULT_CONF_PATH);
        setBaseProps();
    }

    private void setBaseProps() {
        if (!properties.containsKey("zico.log.dir")) {
            properties.setProperty("zico.log.dir", ZicoUtil.ensureDir(ZorkaUtil.path(homeDir, "log")).getPath());
        }

        if (!properties.containsKey("zico.data.dir")) {
            properties.setProperty("zico.data.dir", ZicoUtil.ensureDir(ZorkaUtil.path(homeDir, "data")).getPath());
        }

        if (!properties.containsKey("zico.conf.dir")) {
            properties.setProperty("zico.conf.dir", ZicoUtil.ensureDir(ZorkaUtil.path(homeDir, "conf")).getPath());
        }

        RAGZOutputStream.useLock(boolCfg("rds.lock", true));
    }

    public String getDataDir() {
        return properties.getProperty("zico.data.dir");
    }

    public String getConfDir() {
        return properties.getProperty("zico.conf.dir");
    }
}
