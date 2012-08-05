package com.jitlogic.zorka.integ;

import com.jitlogic.zorka.util.ZorkaLogger;

import javax.management.MBeanServerConnection;
import javax.naming.InitialContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class JBossIntegration extends URLClassLoader {

    private static final ZorkaLogger log = ZorkaLogger.getLogger(JBossIntegration.class);


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
