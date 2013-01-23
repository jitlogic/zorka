/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.mbeans;

import com.jitlogic.zorka.common.ZorkaLog;
import com.jitlogic.zorka.common.ZorkaLogger;

import javax.management.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MBeanServerRegistry maintains all mbean servers seen by zorka agent. Typically there is only
 * platform mbean server (registered as 'java'), but some application servers instantiate their
 * own mbean servers. For example JBoss AS versions 4,5 or 6 maintain additional mbean server
 * (registered as 'jboss' in zorka agent).
 *
 * @author rafal.lewczuk@gmail.com
 */
public class MBeanServerRegistry {

    /** Logger */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** If set to true, platform mbean server will be autmatically registered when referenced for first time. */
    private boolean autoRegister = true;

    /** Represents deferred registration of mbean attributes for not yet registered mbean servers. */
    private static class DeferredRegistration {
        /** mbean server name, object name, attribute name, attribute description */
        public final String name, bean, attr, desc;
        /** Attribute value */
        public final Object obj;
        /** Standard constructor */
        public DeferredRegistration(String name, String bean, String attr, Object obj, String desc) {
            this.name = name;
            this.bean = bean;
            this.attr = attr;
            this.obj = obj;
            this.desc = desc;
        }
    }

    /** Mbean server connections map */
    private Map<String,MBeanServerConnection> conns = new ConcurrentHashMap<String, MBeanServerConnection>();

    /** Accompanying class loaders map */
    private Map<String,ClassLoader> classLoaders = new ConcurrentHashMap<String, ClassLoader>();

    /** Deferred registrations queue */
    private List<DeferredRegistration> deferredRegistrations = new ArrayList<DeferredRegistration>();


    /**
     * Creates new MBeanServerRegistry object.
     *
     * @param autoRegister platform mbean server will be automatically registered when used for the first time
     */
    public MBeanServerRegistry(boolean autoRegister) {
        log.info("Initializing MBeanServerRegistry with autoRegister=" + autoRegister);
        this.autoRegister = autoRegister;
    }


    /**
     * Looks for a given MBean server. java and jboss mbean servers are currently available.
     *
     * @param name mbean server name
     *
     * @return mbean server connection
     */
    public MBeanServerConnection lookup(String name) {
        MBeanServerConnection conn = conns.get(name);
        if (conn == null && autoRegister) {
            if ("java".equals(name)) {
                conn = ManagementFactory.getPlatformMBeanServer();
                conns.put("java", conn);
                registerDeferred(name);
            }
        }

        return conn;
    }

    /**
     * Looks for class loader associated with registered mbean server
     * or null if no class loader is needed.
     *
     * @param name mbean server name
     *
     * @return class loader associated with mbean server or null
     */
    public ClassLoader getClassLoader(String name) {
        return classLoaders.get(name);
    }


    /**
     * Registers mbean server. Any deferred registrations to this mbean server will be performed.
     *
     * @param mbsName mbean server name
     *
     * @param mbsConn mbean server connection
     *
     * @param classLoader class loader associated with mbean server connection (or null)
     */
    public void register(String mbsName, MBeanServerConnection mbsConn, ClassLoader classLoader) {
        synchronized (this) {
            if (!conns.containsKey(mbsName)) {
                conns.put(mbsName, mbsConn);
                if (classLoader != null) {
                    classLoaders.put(mbsName, classLoader);
                }
                registerDeferred(mbsName);
            } else {
                log.error("MBean server '" + mbsName + "' is already registered.");
            }
        }
    }


    /**
     * Unregisters mbean server.
     *
     * @param name mbean server name
     */
    public void unregister(String name) {

        classLoaders.remove(name);

        if (conns.remove(name) == null) {
            log.error("Trying to unregister non-existent MBean server '" + name + "'");
        }

    }


    /**
     * Registers object as mbean server attribute (or return existing one if already registered)
     *
     * @param mbsName mbean server name
     *
     * @param beanName bean name (object name)
     *
     * @param attrName attribtue name
     *
     * @param obj attribute value
     *
     * @param <T> type of attribute value
     *
     * @return attribute value (if any)
     */
    public <T> T getOrRegister(String mbsName, String beanName, String attrName, T obj) {
        return getOrRegister(mbsName, beanName, attrName, obj, attrName);
    }


    /**
     * Registers object as mbean server attribute (or return existing one if already registered)
     *
     * @param mbsName mbean server name
     *
     * @param beanName bean name (object name)
     *
     * @param attrName attribtue name
     *
     * @param desc
     *
     * @param obj attribute value
     *
     * @param <T> type of attribute value
     *
     * @return attribute value (if any)
     */
    public <T> T getOrRegister(String mbsName, String beanName, String attrName, T obj, String desc) {
        MBeanServerConnection mbs = lookup(mbsName);

        // TODO switch class loader if needed

        if (mbs != null) {
            try {
                return (T)mbs.getAttribute(new ObjectName(beanName), attrName);
            } catch (MBeanException e) {
                log.error("Error registering mbean", e);
            } catch (AttributeNotFoundException e) {
                return registerAttr(mbs, beanName, attrName, obj);
            } catch (InstanceNotFoundException e) {
                return registerBeanAttr(mbs, beanName, attrName, obj, desc);
            } catch (ReflectionException e) {
                log.error("Error registering bean", e);
            } catch (IOException e) {
                log.error("Error registering bean", e);
            } catch (MalformedObjectNameException e) {
                log.error("Malformed object name: '" + beanName + "'");
            } catch (ClassCastException e) {
                log.error("Object '" + beanName + "'.'" + attrName + "' of invalid type'", e);
            }
        } else {
            deferredRegistrations.add(new DeferredRegistration(mbsName, beanName, attrName, obj, desc));
            return obj;
        }

        return null;
    }


    /**
     * Registers attribute in mbean server
     *
     * @param conn mbean server connection
     *
     * @param bean bean name (object name)
     *
     * @param attr attribute name
     *
     * @param obj attribute value
     *
     * @param <T> attribute type
     *
     * @return value
     */
    private <T> T registerAttr(MBeanServerConnection conn, String bean, String attr, T obj) {
        try {
            conn.setAttribute(new ObjectName(bean), new Attribute(attr, obj));
        } catch (Exception e) {
            log.error("Error registering object '" + bean + "'.'" + attr + "'", e);
        }
        return obj;
    }


    /**
     * Registers attribute in mbean server.
     *
     * @param conn mbean server connection
     *
     * @param bean bean name (object name)
     *
     * @param attr attribute name
     *
     * @param obj attribute value
     *
     * @param desc attribute description
     *
     * @param <T> attribute type
     *
     * @return value
     */
    private <T> T registerBeanAttr(MBeanServerConnection conn, String bean, String attr, T obj, String desc) {
        ZorkaMappedMBean mbean = new ZorkaMappedMBean(desc);
        mbean.put(attr, obj);
        MBeanServer mbs = (MBeanServer)conn;
        try {
            mbs.registerMBean(mbean, new ObjectName(bean));
        } catch (Exception e) {
            log.error("Error registering object '" + bean + "'.'" + attr + "'", e);
        }
        return obj;
    }


    /**
     * Registers all deferred attributes in an mbean server
     *
     * @param name mbean server name
     */
    private void registerDeferred(String name) {
        if (deferredRegistrations.size() > 0 && conns.containsKey(name)) {
            List<DeferredRegistration> dregs = deferredRegistrations;
            deferredRegistrations = new ArrayList<DeferredRegistration>(dregs.size()+2);
            for (DeferredRegistration dr : dregs) {
                if (name.equals(dr.name)) {
                    getOrRegister(name, dr.bean, dr.attr, dr.obj, dr.desc);
                } else {
                    deferredRegistrations.add(dr);
                }
            }
        }
    }
}