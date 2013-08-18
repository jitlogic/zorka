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
package com.jitlogic.zorka.central;


import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Properties;

public class CentralConfig extends ZorkaConfig {

    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/central/central.properties";

    public CentralConfig(Properties props) {
        properties = props;
        homeDir = props.getProperty("central.home.dir");
        setBaseProps();
    }

    public CentralConfig(String home) {
        loadProperties(home, "central.properties", DEFAULT_CONF_PATH);
        setBaseProps();
    }

    private void setBaseProps() {
        if (!properties.containsKey("central.log.dir")) {
            properties.put("central.log.dir", ZorkaUtil.path(homeDir, "log"));
        }

        if (!properties.containsKey("central.data.dir")) {
            properties.put("central.data.dir", ZorkaUtil.path(homeDir, "data"));
        }
    }
}
