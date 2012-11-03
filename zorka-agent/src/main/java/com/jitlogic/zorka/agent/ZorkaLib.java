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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.jitlogic.zorka.rankproc.AvgRateCounter;
import com.jitlogic.zorka.rankproc.BeanRankLister;
import com.jitlogic.zorka.rankproc.ThreadRankLister;
import com.jitlogic.zorka.mbeans.AttrGetter;
import com.jitlogic.zorka.mbeans.ValGetter;
import com.jitlogic.zorka.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;


/**
 * Standard functions library for zorka-agent.  
 * 
 * @author RLE <rle@jitlogic.com>
 *
 */
public class ZorkaLib implements ZorkaService {
	
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	private ZorkaBshAgent agent;
    private Set<JmxObject> registeredObjects = new HashSet<JmxObject>();
    private ObjectInspector inspector = new ObjectInspector();

    private MBeanServerRegistry mbsRegistry;

    private String hostname = null;


    public ZorkaLib(ZorkaBshAgent agent) {
		this.agent = agent;
        this.mbsRegistry = AgentInstance.getMBeanServerRegistry();
        this.hostname = ZorkaConfig.get("zorka.hostname", "null").trim();
	}


	public String version() {
		return ZorkaBshAgent.VERSION;
	}

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return ""+hostname;
    }
	

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

        Set<ObjectName> names = inspector.queryNames(conn, args.get(1).toString());
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
					for (Object arg : args.subList(3, args.size())) {
						obj = inspector.get(obj, arg);
					}
				}
				objs.add(obj);
			}
            if (cl1 != null) {
                Thread.currentThread().setContextClassLoader(cl0);
            }
		}
		return objs;
	} // jmxList()
	

	
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

        Set<ObjectName> names = inspector.queryNames(conn, argList.get(1).toString());
		
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
            if (cl1 != null)
                Thread.currentThread().setContextClassLoader(cl1);
			obj = conn.getAttribute(name, argList.get(2).toString());
		} catch (AttributeNotFoundException e) {
			log.error("Object '" + conname + "|" + name + 
						"' has no attribute '" + argList.get(2) + "'.");
			return null;
		} catch (Exception e) {
			log.error("Error getting attribute '" + argList.get(2)
				+ "' from '" + conname + "|" + name + "'", e);
		} finally {
            if (cl1 != null)
                Thread.currentThread().setContextClassLoader(cl0);
        }
		
		if (argList.size() > 3 && obj != null) {
			for (Object arg : argList.subList(3, argList.size())) {
				obj = inspector.get(obj, arg);
			}
		}
		
		return obj;
	} // jmx()


    /**
     * Recursively accesses object.
     *
     * @param obj
     * @param args
     * @return
     */
	public Object get(Object obj, Object...args) {
		for (Object arg : args) {
			obj = inspector.get(obj, arg);
		}
		return obj;
	}
	
	
	public ZorkaMappedMBean mbean(String mbs, String name) { 
		return mbean(mbs, name, "");
	}
	
	
	public ZorkaMappedMBean mbean(String mbs, String name, String desc) {
        // TODO wyrugować ręczne tworzenie mbeanów, zdefiniować jedną (wspólną) metodę do tego i używać jej
		try {
			ZorkaMappedMBean mbean = new ZorkaMappedMBean(desc);
			MBeanServer conn = (MBeanServer) mbsRegistry.lookup(mbs);
			if (conn == null) {
				throw new ZorkaException("There is no mbean server named '" + mbs + "'");
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
    public ValGetter getter(Object obj, String...attrs) {
        return new AttrGetter(obj, attrs);
    }

	
	// TODO move this to "initialization library" script (?)
	public ZorkaMappedMBean threadRanking(String name, String desc, int size, long rerankInterval) {
		long updateInterval = 15000;
		//String bname = "zorka.jvm:name = " + name + ",type=ThreadMonitor,updateInterval=" + updateInterval + ",rerankInterval=" + rerankInterval;
		String bname = name;
		
		ZorkaMappedMBean bean = (ZorkaMappedMBean)jmx("java", bname, "this"); 
		if (bean != null) { return bean; }
		
		bean = mbean("java", bname, desc);
		
		ThreadRankLister tl = new ThreadRankLister(updateInterval, rerankInterval);
		tl.newAttr("cpu1",  "CPU utilization 1-minute average",     60000, 100, "cpuTime", "tstamp");
		tl.newAttr("cpu5",  "CPU utilization 5-minute average",   5*60000, 100, "cpuTime", "tstamp");
		tl.newAttr("cpu15", "CPU utilization 15-minute average", 15*60000, 100, "cpuTime", "tstamp");
		tl.newAttr("block1",  "Blocked time percentage 1-minute average",    60000, 100, "blockedTime", "tstamp");
		tl.newAttr("block5",  "Blokced time percentage 5-minute average",  5*60000, 100, "blockedTime", "tstamp");
		tl.newAttr("block15", "Blokced time percentage 15-minute average", 5*60000, 100, "blockedTime", "tstamp");
		
		for (String attr : new String[]{ "cpu1", "cpu5", "cpu15", "block1", "block5", "block15" }) { 
			bean.put(attr, tl.newList(attr, attr, size));
		}
		
		agent.svcAdd(tl);
		
		return bean; 
	}	
	
	
	// TODO move this to "initialization library" script (?)
	public ZorkaMappedMBean beanRanking(String mbs, String bname, String query, String keyName, String attrs, String nominalAttr, String dividerAttr, int size, long rerankInterval) {
		long updateInterval = 15000;
        MBeanServerConnection conn = mbsRegistry.lookup(mbs);

        if (conn == null)  {
			log.error("There is no mbean server named '" + mbs + "'");
			return null;
		}
		
		ZorkaMappedMBean bean = (ZorkaMappedMBean)jmx("java", bname, "this");
		if (bean != null) { return bean; }
		
		bean = mbean(mbs, bname, "");
		
		BeanRankLister bl = new BeanRankLister(updateInterval, rerankInterval, conn, query, keyName, attrs.split(","), nominalAttr, dividerAttr);
		bl.newAttr("avg1", "1-minute average", 60000, 1, nominalAttr, dividerAttr);
		bl.newAttr("avg5", "5-minutes average", 5*60000, 1, nominalAttr, dividerAttr);
		bl.newAttr("avg15", "15-minute average", 15*60000, 1, nominalAttr, dividerAttr);
		
		for (String attr : new String[]{ "avg1", "avg5", "avg15" }) {
			bean.put(attrs,  bl.newList(attr, attr, size));
		}
		
		return bean;
	}

    private AvgRateCounter rateCounter = new AvgRateCounter(this);

    public Double rate(Object...args) {

        if (args.length < 5) {
            log.error("Too little arguments for zorka.rate(). At least 5 args are required");
            return null;
        }

        Object oh = args[args.length-1];
        long horizon = 0;

        if (oh instanceof String && ((String) oh).matches("^AVG[0-9]+$")) {
            horizon = Long.parseLong(oh.toString().substring(3)) * 60000;
        } else {
            horizon = rateCounter.coerce(args[args.length-1]) * 1000;
        }

        if (horizon == 0) {
            log.error("Invalid time horizon in zorka.rate()");
            return null;
        }

        String div = (String)args[args.length-2];
        String nom = (String)args[args.length-3];

        List<Object> path = new ArrayList<Object>(args.length-3);

        for (int i = 0; i < args.length-3; i++) {
            path.add(args[i]);
        }

        return rateCounter.get(path, nom, div, horizon);
    }

    public ZorkaLog log(String tag) {
        return ZorkaLogger.getLog(tag);
    }


    public void reload(String mask) {
        agent.loadScriptDir(ZorkaConfig.getConfDir(),
            "^"+mask.replace("\\.", "\\\\.").replace("*", ".*")+"$");
    }


	public void svcStart() {
	}
	
	
	public void svcStop() {
		for (JmxObject obj : registeredObjects) {
			try {
				obj.getConn().unregisterMBean(obj.getName());
			} catch (Exception e) {
				log.error("Error unregistering bean " + obj.getName(), e);
			}
		}
	} // svcStop()
	
	
	public void svcClear() {
	}
	
	
	public void svcReload() {
	}
	
	
	public String reload() {
		agent.getExecutor().execute(
		new Runnable() {
			public void run() {
				try { Thread.sleep(5); } catch (InterruptedException e) { }
				agent.svcReload();
			}
		});
		return "OK";
	}

    public void registerMbs(String name, MBeanServerConnection mbs) {
        mbsRegistry.register(name, mbs, mbs.getClass().getClassLoader());
    }

}
