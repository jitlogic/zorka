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

import com.jitlogic.zorka.central.CentralApp;
import com.jitlogic.zorka.central.CentralConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.ProtectionDomain;


public class CentralMain {

    public static void main(String[] args) throws Exception {

        CentralConfig config = CentralApp.getInstance().getConfig();

        InetSocketAddress addr = new InetSocketAddress(
                config.stringCfg("central.http.addr", "0.0.0.0"),
                config.intCfg("central.http.port", 8080));

        Server server = new Server(addr);
        ProtectionDomain domain = Server.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        webapp.setServer(server);
        webapp.setWar(location.toExternalForm());

        webapp.setTempDirectory(new File("/tmp"));

        server.setHandler(webapp);
        server.start();
        server.join();
    }

}
