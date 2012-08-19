/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.integ;

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import javax.management.MBeanServerConnection;
import javax.naming.InitialContext;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class JBossIntegration extends URLClassLoader {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());


    public JBossIntegration() {
        super(new URL[]{});
        initialize();
    }


    private void initialize() {
        String path = "/opt/ztest/jboss-5.1.0.GA/client/jbossall-client.jar";
        try {
            addURL(new URL("file:" + path));
        } catch (Exception e) {
            log.error("Error loading JBoss jars", e);
        }
    }


    public synchronized MBeanServerConnection getJBossMBeanServer() {
        ClassLoader syscl = Thread.currentThread().getContextClassLoader();
        try {
            //Thread.currentThread().setContextClassLoader(this);

            InitialContext ctx = new InitialContext();
            MBeanServerConnection server =
                    (MBeanServerConnection)ctx.lookup("/jmx/rmi/RMIAdaptor");
            return server;
        } catch (Exception e) {
            log.error("Cannot find MBeanServer for JBoss: ", e);
        } catch (NoClassDefFoundError e) {
            log.error("Cannot find MBeanServer for JBoss: ", e);
        } finally {
            Thread.currentThread().setContextClassLoader(syscl);
        }
        return null;
    }


}
