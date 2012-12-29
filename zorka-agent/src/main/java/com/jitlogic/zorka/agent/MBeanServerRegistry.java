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

package com.jitlogic.zorka.agent;

import com.jitlogic.zorka.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;

import javax.management.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MBeanServerRegistry {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private boolean autoRegister = true;

    private static class DeferredRegistration {
        public final String name, bean, attr, desc;
        public final Object obj;
        public DeferredRegistration(String name, String bean, String attr, Object obj, String desc) {
            this.name = name; this.bean = bean; this.attr = attr; this.obj = obj; this.desc = desc;
        }
    }

    private Map<String,MBeanServerConnection> conns = new ConcurrentHashMap<String, MBeanServerConnection>();
    private Map<String,ClassLoader> classLoaders = new ConcurrentHashMap<String, ClassLoader>();

    private List<DeferredRegistration> deferredRegistrations = new ArrayList<DeferredRegistration>();


    public MBeanServerRegistry(boolean autoRegister) {
        log.info("Initializing MBeanServerRegistry with autoRegister=" + autoRegister);
        this.autoRegister = autoRegister;
    }


    /**
     * Looks for a given MBean server. java and jboss mbean servers are currently available.
     *
     * @param name
     * @return
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

    public ClassLoader getClassLoader(String name) {
        return classLoaders.get(name);
    }

    public synchronized void register(String mbsName, MBeanServerConnection mbsConn, ClassLoader classLoader) {
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


    public synchronized void unregister(String name) {
        if (conns.containsKey(name)) {
            conns.remove(name);
        } else {
            log.error("Trying to unregister non-existent MBean server '" + name + "'");
        }

        classLoaders.remove(name);
    }


    public <T> T getOrRegister(String mbsName, String beanName, String attrName, T obj) {
        return getOrRegister(mbsName, beanName, attrName, obj, attrName);
    }


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


    private <T> T registerAttr(MBeanServerConnection conn, String bean, String attr, T obj) {
        try {
            conn.setAttribute(new ObjectName(bean), new Attribute(attr, obj));
        } catch (Exception e) {
            log.error("Error registering object '" + bean + "'.'" + attr + "'", e);
        }
        return obj;
    }


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