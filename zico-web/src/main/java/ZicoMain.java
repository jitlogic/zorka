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


import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Properties;


public class ZicoMain {

    private static int port;
    private static String homeDir;

    private static Server server;
    private static WebAppContext webapp;


    public static void main(String[] args) throws Exception {

        configure();

        initServer();

        server.start();
        server.join();
    }

    private static void initServer() {
        server = new Server(port);
        ProtectionDomain domain = Server.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        webapp.setServer(server);
        webapp.setWar(location.toExternalForm());
        webapp.setTempDirectory(new File(homeDir, "tmp"));

        server.setHandler(webapp);
    }

    private static void configure() throws IOException {

        homeDir = System.getProperty("zico.home.dir");

        if (homeDir == null) {
            System.err.println("ERROR: Missing home dir property: add -Dzico.home.dir=<path-to-collector-home> to JVM args.");
            System.exit(1);
        }

        String strPort = System.getProperty("zico.http.port", "8642").trim();

        Properties props = new Properties();

        InputStream fis = null;
        try {
            fis = new FileInputStream(new File(homeDir, "zico.properties"));
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Cannot open zico.properties file: " + e.getMessage());
            System.exit(1);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        strPort = props.getProperty("zico.http.port", strPort).trim();

        try {
            port = Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            System.err.println("Invalid HTTP port setting (not a number): " + strPort);
            System.exit(1);
        }
    }

}
