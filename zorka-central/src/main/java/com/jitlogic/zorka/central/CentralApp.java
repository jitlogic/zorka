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

import java.io.File;


public class CentralApp {
    private static volatile CentralInstance instance = null;

    public static synchronized CentralInstance getInstance() {
        if (instance == null) {
            String homeDir = System.getProperty("central.home.dir");
            if (homeDir == null) {
                throw new RuntimeException("Missing home dir configuration property. " +
                        "Add '-Dcentral.home.dir=/path/to/zorka/central' to JVM options.");
            }
            if (!new File(homeDir).isDirectory()) {
                throw new RuntimeException("Home dir property does not point to a directory.");
            }
            CentralConfig config = new CentralConfig(homeDir);
            System.out.println("Database URL: " + config.stringCfg("central.db.url", "??"));
            instance = new CentralInstance(config);
            instance.start();
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        }
        return instance;
    }


    private static class ShutdownHook extends Thread {
        public void run() {
            if (instance != null) {
                System.out.println("Shutting down Zorka Central ...");
                instance.stop();
            }
        }
    }
}
