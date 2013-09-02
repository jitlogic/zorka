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

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Properties;


public class CentralMain {

    private static String addr;
    private static int port;
    private static String homeDir;
    private static Server server;
    private static WebAppContext webapp;


    public static void main(String[] args) throws Exception {

        configure();

        initServer();

        initSecurity();

        server.start();
        server.join();
    }

    private static void initServer() {
        server = new Server(new InetSocketAddress(addr, port));
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

    private static void initSecurity() {
        File config = new File(homeDir, "users.properties");

        if (!config.exists()) {
            System.err.println("ERROR: Missing users.properties file in " + homeDir);
            System.exit(1);
        }

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__FORM_AUTH);
        constraint.setRoles(new String[]{"OPER"});
        constraint.setAuthenticate(true);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/*");

        FormAuthenticator authenticator = new FormAuthenticator("/login.html", "/login-fail.html", false);

        LoginService loginService = new HashLoginService("Zorka Central UI", config.getPath());

        ConstraintSecurityHandler handler = new ConstraintSecurityHandler();
        handler.addConstraintMapping(mapping);
        handler.setLoginService(loginService);
        handler.setAuthenticator(authenticator);

        webapp.setSecurityHandler(handler);
    }

    private static void configure() throws IOException {

        homeDir = System.getProperty("central.home.dir");

        if (homeDir == null) {
            System.err.println("ERROR: Missing home dir property: add -Dcentral.home.dir=<path-to-central-home> to JVM args.");
            System.exit(1);
        }

        addr = System.getProperty("central.http.addr", "0.0.0.0").trim();
        String strPort = System.getProperty("central.http.port", "8642").trim();

        Properties props = new Properties();

        InputStream fis = null;
        try {
            fis = new FileInputStream(new File(homeDir, "central.properties"));
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Cannot open central.properties file: " + e.getMessage());
            System.exit(1);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        addr = props.getProperty("central.http.addr", addr).trim();
        strPort = props.getProperty("central.http.port", strPort).trim();

        try {
            port = Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            System.err.println("Invalid HTTP port setting (not a number): " + strPort);
            System.exit(1);
        }
    }

}
