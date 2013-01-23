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

package com.jitlogic.zorka.agent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.*;

import com.jitlogic.zorka.agent.integ.FileTrapper;
import com.jitlogic.zorka.common.ZorkaLog;
import com.jitlogic.zorka.common.ZorkaLogLevel;
import com.jitlogic.zorka.common.ZorkaLogger;
import com.jitlogic.zorka.agent.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.agent.rankproc.*;
import com.jitlogic.zorka.agent.mbeans.AttrGetter;
import com.jitlogic.zorka.common.ValGetter;
import com.jitlogic.zorka.agent.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.common.*;


/**
 * Standard library for zorka-agent. All public methods implemented in this module will be available for
 * zorka configuration scripts in 'zorka' namespace.
 * 
 * @author rafal.lewczuk@jitlogic.com
 *
 */
public class ZorkaLib  {

    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaLogger.class);

    public static final ZorkaLogLevel TRACE = ZorkaLogLevel.TRACE;
    public static final ZorkaLogLevel DEBUG = ZorkaLogLevel.DEBUG;
    public static final ZorkaLogLevel INFO  = ZorkaLogLevel.INFO;
    public static final ZorkaLogLevel WARN  = ZorkaLogLevel.WARN;
    public static final ZorkaLogLevel ERROR = ZorkaLogLevel.ERROR;
    public static final ZorkaLogLevel FATAL = ZorkaLogLevel.FATAL;

    private static final int MINUTE = 60000;
    private static final int SECOND = 1000;

    private ZorkaLogger logger = ZorkaLogger.getLogger();
	
	private ZorkaBshAgent agent;
    private Set<JmxObject> registeredObjects = new HashSet<JmxObject>();

    private MBeanServerRegistry mbsRegistry;

    private String hostname;

    private AvgRateCounter rateCounter = new AvgRateCounter(this);
    private Map<String, FileTrapper> fileTrappers = new ConcurrentHashMap<String, FileTrapper>();

    private TaskScheduler scheduler = TaskScheduler.instance();

    /**
     * Standard constructor
     *
     * @param agent reference to Zorka BSH agent object
     */
    public ZorkaLib(ZorkaBshAgent agent) {
		this.agent = agent;
        this.mbsRegistry = AgentInstance.getMBeanServerRegistry();
        this.hostname = ZorkaConfig.getProperties().getProperty("zorka.hostname").trim();
	}


    /**
     * Returns agent version.
     *
     * @return string containing agent version
     */
	public String version() {
		return ZorkaConfig.getProperties().getProperty("zorka.version").trim();
	}


    /**
     * Returns hostname (name agent will present to monitoring systems communicating with agent).
     * Note that this does have to be host name of server running application server hosting this agent.
     * Administrator can configure many JVMs on a single server and each can present different name.
     * Agent hostname can be configured in zorka.properties file as 'zorka.hostname' property.
     *
     * By convention 'app-name.os-hostname.domain' should be used.
     *
     * @return hostname string
     */
    public String getHostname() {
        return ""+hostname;
    }


    /**
     * Returns list of objects from given mbean server.
     *
     * @param args attribute chain (as in ObjectInspector.get() function)
     *
     * @return list of objects
     *
     * TODO fix parameters of this method
     */
	public List<Object> jmxList(List<Object> args) {
		List<Object> objs = new ArrayList<Object>();
		if (args.size() < 2) {
			log.error("Zorka JMX function takes at least 2 arguments.");
			return objs;
		}
        String conname = args.get(0).toString();
        MBeanServerConnection conn = mbsRegistry.lookup(conname);
		if (conn == null) {
			log.error("MBean server named '" + args.get(0) + "' is not registered.");
			return objs;
		}
        ClassLoader cl0 = Thread.currentThread().getContextClassLoader(), cl1 = mbsRegistry.getClassLoader(conname);

        Set<ObjectName> names = ObjectInspector.queryNames(conn, args.get(1).toString());
		if (args.size() == 2) {
			for (ObjectName name : names) {
				objs.add(new JmxObject(name, conn, cl1));
			}
		} else {
            if (cl1 != null) {
                Thread.currentThread().setContextClassLoader(cl1);
            }
			for (ObjectName name : names) {
				Object obj = null;
				try {
					obj = conn.getAttribute(name, args.get(2).toString());
				} catch (AttributeNotFoundException e) {
					log.error("Object '" + conname + "|" + name + 
						"' has no attribute '" + args.get(2) + "'.", e);
				} catch (Exception e) {
					log.error("Error getting attribute '" + args.get(2) 
						+ "' from '" + conname + "|" + name + "'", e);
				}

				if (args.size() > 3) {
                    obj = ObjectInspector.get(obj, args.subList(3, args.size()).toArray(new Object[0]));
				}
				objs.add(obj);
			}
            if (cl1 != null) {
                Thread.currentThread().setContextClassLoader(cl0);
            }
		}
		return objs;
	} // jmxList()


    /**
     * Return text dump of selected JMX objects
     *
     * @param args attribute chain (as in ObjectInspector.get() function)
     *
     * @return dump of selected JMX objects
     * TODO fix parameters of this method
     */
    public String dump(Object...args) {

        StringBuffer sb = new StringBuffer();

        for (Object obj : jmxList(Arrays.asList(args))) {
            sb.append(ObjectDumper.objectDump(obj));
        }

        return sb.toString();
    } // dump()


    /**
     * Retrieves object from JMX
     *
     * @param args attribute chain (as in ObjectInspector.get() function)
     *
     * @return retrieved obejct
     */
	public Object jmx(Object...args) {

        List<Object> argList = Arrays.asList(args);

		if (argList.size() < 2) {
			log.error("zorka.jmx() function requires at least 2 arguments");
			return null;
		}
		
		String conname = argList.get(0).toString();

        MBeanServerConnection conn = mbsRegistry.lookup(conname);

        if (conn == null) {
			log.error("MBean server named '" + argList.get(0) + "' is not registered.");
			return null;
		}

        Set<ObjectName> names = ObjectInspector.queryNames(conn, argList.get(1).toString());
		
		if (names.isEmpty()) { 
			return null;
		}
		
		ObjectName name = names.iterator().next();

        ClassLoader cl0 = Thread.currentThread().getContextClassLoader(), cl1 = mbsRegistry.getClassLoader(conname);

        if (argList.size() == 2) {
			return new JmxObject(name, conn, cl1);
		}
		
		Object obj = null;
		try {
            if (cl1 != null) {
                Thread.currentThread().setContextClassLoader(cl1);
            }
			obj = conn.getAttribute(name, argList.get(2).toString());
		} catch (AttributeNotFoundException e) {
			log.error("Object '" + conname + "|" + name + 
						"' has no attribute '" + argList.get(2) + "'.");
			return null;
		} catch (Exception e) {
			log.error("Error getting attribute '" + argList.get(2)
				+ "' from '" + conname + "|" + name + "'", e);
		} finally {
            if (cl1 != null) {
                Thread.currentThread().setContextClassLoader(cl0);
            }
        }
		
		if (argList.size() > 3 && obj != null) {
            obj = ObjectInspector.get(obj, argList.subList(3, argList.size()).toArray(new Object[0]));
		}
		
		return obj;
	} // jmx()


    // TODO expand ls functionality to filter over all arguments usign regex;
    // TODO converge zorka.ls and zabbix.discover call conventions
    // TODO converge zorka.jmxList and zorka.list
    // TODO split zorka.ls into zorka.ls [returning list of strings] and zorka.list() [returning list of lists]

    /**
     * Lists attributes of given object(s)
     *
     * @param mbsName mbean server name
     *
     * @param objectName object name (mask)
     *
     * @param args attribute chain (as in ObjectInspector.get())
     *
     * @return string listing attributes and their values
     */
    public String ls(String mbsName, String objectName, Object...args) {
        MBeanServerConnection conn = mbsRegistry.lookup(mbsName);

        if (conn == null) {
            log.error("MBean server named '" + mbsName + "' is not registered.");
            return null;
        }

        Set<ObjectName> names = ObjectInspector.queryNames(conn, objectName);
        List<String> rslt = new ArrayList<String>(32);

        ClassLoader cl0 = Thread.currentThread().getContextClassLoader(), cl1 = mbsRegistry.getClassLoader(mbsName);

        if (cl1 != null) {
            Thread.currentThread().setContextClassLoader(cl1);
        }

        for (ObjectName name : names) {
            if (args.length == 0) {
                rslt.add(name.toString());
            } else {
                try {
                    Object obj = null;
                    String path = name.toString() + ": ";
                    if (args.length == 1 && "*".equals(args[0])) {
                        obj = new JmxObject(name, conn, cl1);
                    } else {
                        obj = conn.getAttribute(name, args[0].toString());
                        path =  path + ZorkaUtil.join(".", Arrays.asList(args)) + ".";
                    }

                    if (args.length > 1) {
                        for (int i = 1; i < args.length; i++) {
                            obj = ObjectInspector.get(obj, name);
                        }
                    }


                    for (Object attr : ObjectInspector.list(obj)) {
                        rslt.add(path + attr + " -> " + ObjectInspector.get(obj, attr));
                    }

                } catch (Exception e) {
                    log.error("Cannot resolve '" + name + "." + args[0], e);
                }
            }
        }

        if (cl1 != null) {
            Thread.currentThread().setContextClassLoader(cl0);
        }

        Collections.sort(rslt);

        return ZorkaUtil.join("\n", rslt) + "\n";
    } // ls()


    /**
     * Recursively accesses object. This is just a ObjectInspector.get() method facade for configuration scripts.
     *
     * @param obj source object
     *
     * @param args attribute chain
     *
     * @return retrieved value
     */
	public Object get(Object obj, Object...args) {
        return ObjectInspector.get(obj, args);
	}


    /**
     * Creates zorka dynamic MBean. Such mbean object can be populated with attributes
     * using put() or setAttr() methods.
     *
     * @param mbs mbean server at which bean will register
     *
     * @param name object name
     *
     * @return mbean object
     */
	public ZorkaMappedMBean mbean(String mbs, String name) { 
		return mbean(mbs, name, "");
	}


    /**
     * Creates zorka dynamic MBean. Such mbean object can be populated with attributes
     *
     * @param mbs mbean server at which bean will register
     *
     * @param name object name
     *
     * @param desc mbean description
     *
     * @return mbean object
     */
	public ZorkaMappedMBean mbean(String mbs, String name, String desc) {
        // TODO wyrugować ręczne tworzenie mbeanów, zdefiniować jedną (wspólną) metodę do tego i używać jej
		try {
			ZorkaMappedMBean mbean = new ZorkaMappedMBean(desc);
			MBeanServer conn = (MBeanServer) mbsRegistry.lookup(mbs);
			if (conn == null) {
				throw new IllegalArgumentException("There is no mbean server named '" + mbs + "'");
			}
			
			ObjectName on = new ObjectName(name);
			conn.registerMBean(mbean, on);
			registeredObjects.add(new JmxObject(on, conn, Thread.currentThread().getContextClassLoader()));
			return mbean;
		} catch (Exception e) {
			// TODO zalogowac problem
			return null;
		}
	}


    /**
     * Creates Attribute getter object.
     *
     * @param obj
     * @param attrs
     * @return
     */
    public ValGetter getter(Object obj, Object...attrs) {
        return new AttrGetter(obj, attrs);
    }


    /**
     * Calculates windowed rate of two associated parameters (eg. execution time and number of calls).
     * This is useful for statistics that only provide total values (and no averages). It requires
     * caller to periodically call this function in order to maintain collected deltas, so this method
     * will be deprecated as soon as viable alternative appears.
     *
     * @param args first two arguments are mbean server name and object name, last three arguments are
     *             nominator field, divider field and time horizon, all remaining middle arguemnts are
     *             optional and are part of attribute chain needed to reach from mbean to proper object
     *             containing statistic
     *
     * @return calculated rate
     */
    public Double rate(Object...args) {

        if (args.length < 5) {
            log.error("Too little arguments for zorka.rate(). At least 5 args are required");
            return null;
        }

        Object oh = args[args.length-1];
        long horizon = 0;

        if (oh instanceof String && ((String) oh).matches("^AVG[0-9]+$")) {
            horizon = Long.parseLong(oh.toString().substring(3)) * MINUTE;
        } else {
            horizon = rateCounter.coerce(args[args.length-1]) * SECOND;
        }

        if (horizon == 0) {
            log.error("Invalid time horizon in zorka.rate()");
            return null;
        }

        String div = (String)args[args.length-2];
        String nom = (String)args[args.length-3];

        return rateCounter.get(Arrays.asList(ZorkaUtil.clipArray(args, args.length-3)), nom, div, horizon);
    }


    /**
     * Sends DEBUG message to zorka log.
     *
     * @param message message text
     *
     * @param args optional arguments (if message contains format string markers)
     */
    public void logDebug(String message, Object...args) {
        log(ZorkaLogLevel.DEBUG, message, args);
    }

    /**
     * Sends INFO message to zorka log.
     *
     * @param message message text
     *
     * @param args optional arguments (if message contains format string markers)
     */
    public void logInfo(String message, Object...args) {
        log(ZorkaLogLevel.INFO, message, args);
    }

    /**
     * Sends WARNING message to zorka log.
     *
     * @param message message text
     *
     * @param args optional arguments (if message contains format string markers)
     */
    public void logWarning(String message, Object...args) {
        log(ZorkaLogLevel.WARN, message, args);
    }

    /**
     * Sends ERROR message to zorka log.
     *
     * @param message message text
     *
     * @param args optional arguments (if message contains format string markers)
     */
    public void logError(String message, Object...args) {
        log(ZorkaLogLevel.ERROR, message, args);
    }

    /**
     * Sends FATAL message to zorka log.
     *
     * @param message message text
     *
     * @param args optional arguments (if message contains format string markers)
     */
    public void logFatal(String message, Object...args) {
        log(ZorkaLogLevel.ERROR, message, args);
    }

    /**
     * Logs arbitrary message to zorka log.
     *
     * @param level log level
     *
     * @param message message text
     *
     * @param argv optional arguments (if message is a format string)
     */
    private void log(ZorkaLogLevel level, String message, Object...argv) {
        Throwable ex = null;
        Object[] args = argv;
        if (args.length > 0 && args[args.length-1] instanceof Throwable) {
            ex = (Throwable)args[args.length-1];
            args = ZorkaUtil.clipArray(args, -1);
        }
        logger.trap(level, "<script>", message, ex, args);
    }


    /**
     * Reloads and executes configuration scripts matching given mask.
     * Scripts have to be in $ZORKA_HOME/conf directory.
     *
     * @param mask file name or file mask.
     */
    public void reload(String mask) {
        agent.loadScriptDir(ZorkaConfig.getConfDir(),
                "^" + mask.replace("\\.", "\\\\.").replace("*", ".*") + "$");
    }

    /**
     * Registers mbean server in agent mbean server registry. It is useful for some
     * application servers that have non-standard (additional) mbean servers or when
     * agent cannot access platform mbean server at start up time (notably JBoss 4/5/6/7).
     *
     * @param name name at which mbean server will be registered
     *
     * @param mbs reference to mbean server connection
     */
    public void registerMbs(String name, MBeanServerConnection mbs) {
        mbsRegistry.register(name, mbs, mbs.getClass().getClassLoader());
    }


    /**
     * Presents object as an attribute of an mbean. If there is already such attribute, its value
     * will be returned instead. If mbean exists, it must be ZorkaMappedBean.
     *
     * @param mbsName mbean server name
     * @param beanName object name
     * @param attrName attribute name
     * @param obj object to be presented
     * @param <T> type of object to be presented
     * @return obj if there was no such attribute or value of existing attribute
     */
    public <T> T registerAttr(String mbsName, String beanName, String attrName, T obj) {
        return mbsRegistry.getOrRegister(mbsName, beanName, attrName, obj);
    }

    /**
     * Presents object as an attribute of an mbean. If there is already such attribute, its value
     * will be returned instead. If mbean exists, it must be ZorkaMappedBean.
     *
     * @param mbsName mbean server name
     * @param beanName object name
     * @param attrName attribute name
     * @param obj object to be presented
     * @param <T> type of object to be presented
     * @param desc attribute description
     *
     * @return obj if there was no such attribute or value of existing attribute
     */
    public <T> T registerAttr(String mbsName, String beanName, String attrName, T obj, String desc) {
        return mbsRegistry.getOrRegister(mbsName, beanName, attrName, obj, desc);
    }


    /**
     * Creates JMX lister object that can be used to create rankings
     *
     * @param mbsName mbean server name
     *
     * @param onMask object name (or mask)
     *
     * @param <T> wrapped object type (if any)
     *
     * @return JMX rank lister object
     */
    public <T extends Rankable<?>> RankLister<T> jmxLister(String mbsName, String onMask) {
        JmxAggregatingLister<T> lister = new JmxAggregatingLister<T>(mbsName, onMask);
        return lister;
    }


    /** Thread rank lister (if it has been created) */
    private ThreadRankLister threadRankLister;


    /**
     * Returns thread rank lister. Creates and starts a new one if none has been creater (yet).
     * As thread lister causes some stress on JVM, it is a singleton object and it is created in lazy manner.
     *
     * @return thread rank lister object
     */
    public ThreadRankLister threadRankLister() {
        synchronized (this) {
            if (threadRankLister == null) {
                threadRankLister = new ThreadRankLister();
                scheduler.schedule(threadRankLister, 15000);
            }
        }

        return threadRankLister;
    }

    /**
     * Creates EJB rank lister object that can be used to create rankings.
     * It will list EJB statistics from selected mbeans.
     *
     * @param mbsName mbean server name
     *
     * @param objNames object names
     *
     * @param attr attribute name
     *
     * @return EJB rank lister object
     */
    public EjbRankLister ejbRankLister(String mbsName, String objNames, String attr) {
        EjbRankLister lister = new EjbRankLister(mbsName, objNames, attr);
        scheduler.schedule(lister, 15000);
        return lister;
    }


    /**
     * Looks for file trapper and returns if trapper exists.
     *
     * @param id trapper ID
     *
     * @return file trapper or null
     */
    public FileTrapper fileTrapper(String id) {
        return fileTrappers.get(id);
    }


    /**
     * Loooks for file trapper registered as 'id' or creates and registers rolling file trapper.
     *
     * @param id trapper ID
     *
     * @param logLevel log level (only messages with such or higher log level will be logged)
     *
     * @param path path to log file (excluding numbered suffixes)
     *
     * @param count number of archived files (excluding current one)
     *
     * @param maxSize maximum file size
     *
     * @param logExceptions if true, stack traces of passed exceptions will be logged
     *
     * @return file trapper
     */
    public FileTrapper rollingFileTrapper(String id, String logLevel, String path, int count, long maxSize, boolean logExceptions) {
        FileTrapper trapper = fileTrappers.get(id);

        if (trapper == null) {
            trapper = FileTrapper.rolling(ZorkaLogLevel.valueOf(logLevel), propFormat(path), count, maxSize, logExceptions);
            trapper.start();
            fileTrappers.put(id, trapper);
        }

        return trapper;
    }


    /**
     * Looks for file trapper registered as 'id' or creates and registers daily file trapper.
     *
     * @param id trapper ID
     *
     * @param logLevel trapper log level (only messages with such or higher log level will be logged)
     *
     * @param path path to log file (excluding suffix indicating log date)
     *
     * @param logExceptions if true, trapper will log stack traces of passed exceptions
     *
     * @return file trapper
     */
    public FileTrapper dailyFileTrapper(String id, ZorkaLogLevel logLevel, String path, boolean logExceptions){
        FileTrapper trapper = fileTrappers.get(id);

        if (trapper == null) {
            trapper = FileTrapper.daily(logLevel, propFormat(path), logExceptions);
            trapper.start();
            fileTrappers.put(id, trapper);
        }

        return trapper;
    }


    /**
     * Stops and unregisters file trapper
     *
     * @param id trapper ID
     */
    public void removeFileTrapper(String id) {
        FileTrapper trapper = fileTrappers.get(id);

        if (trapper != null) {
            trapper.stop();
        }
    }


    /**
     * Formats string containing references to zorka properties.
     *
     * @param input zorka properties
     *
     * @return
     */
    public String propFormat(String input) {
        return ZorkaConfig.propFormat(input);
    }



    /**
     * Schedules a task.
     *
     * @param task task (must be Runnable)
     *
     * @param interval run interval (in milliseconds)
     */
    public void schedule(Runnable task, long interval) {

    }
}
