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
package com.jitlogic.zico.main;


import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zorka.common.util.ZorkaConfig;

import java.io.File;
import java.util.Properties;

public class ZicoHostStoreCheckCommand implements ZicoCommand {

    private ZicoConfig config;

    @Override
    public void run(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Host check command requires at least ZICO home dir and one host name.");
            return;
        }

        Properties props = ZorkaConfig.defaultProperties(ZicoConfig.DEFAULT_CONF_PATH);
        props.setProperty("zico.home.dir", args[1]);

        config = new ZicoConfig(props);

        for (int i = 2; i < args.length; i++) {
            String h = args[i];
            File fhs = new File(config.getDataDir(), h);
            if (!fhs.isDirectory()) {
                System.err.println("Host " + h + " does not exist. Skipping.");
                continue;
            }
            HostStore hs = new HostStore(config, null, h);
            hs.rebuildIndex();
        }

    }

}
