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

import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class CentralApp extends HttpServlet {
    private static volatile CentralInstance instance = null;


    public void init(ServletConfig config) throws ServletException {
        configureInstance();
    }


    public static synchronized CentralInstance getInstance() {
        if (instance == null) {
            configureInstance();
        }
        return instance;
    }


    private static synchronized void configureInstance() {
        // Redirect java.util.logging to slf4j
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        Logger.getLogger("global").setLevel(Level.FINEST);

        String homeDir = System.getProperty("central.home.dir");

        if (homeDir == null) {
            throw new RuntimeException("Missing home dir configuration property. " +
                    "Add '-Dcentral.home.dir=/path/to/zorka/central' to JVM options.");
        }

        if (!new File(homeDir).isDirectory()) {
            throw new RuntimeException("Home dir property does not point to a directory.");
        }

        CentralConfig config = new CentralConfig(homeDir);
        instance = new CentralInstance(config);
        instance.start();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }


    public static synchronized void setInstance(CentralInstance instance) {
        CentralApp.instance = instance;
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
