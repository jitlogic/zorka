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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.jitlogic.zorka.agent.rankproc.BeanRankLister;
import com.jitlogic.zorka.agent.rankproc.ThreadRankLister;
import com.jitlogic.zorka.mbeans.AttrGetter;
import com.jitlogic.zorka.mbeans.ValGetter;
import com.jitlogic.zorka.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.util.ZorkaLogger;


/**
 * Standard functions library for zorka-agent.  
 * 
 * @author RLE <rle@jitlogic.com>
 *
 */
public class ZorkaLib implements ZorkaService {
	
	private static final ZorkaLogger log = ZorkaLogger.getLogger(ZorkaLib.class);
	
	private ZorkaBshAgent agent;
	private Map<String, MBeanServerConnection> conns = new HashMap<String, MBeanServerConnection>();
	private Set<JmxObject> registeredObjects = new HashSet<JmxObject>();
	private JmxResolver resolver = new JmxResolver();
	
	public ZorkaLib(ZorkaBshAgent agent) {
		this.agent = agent;
	}
	

	
	public void addServer(String name, MBeanServerConnection conn) {
		// TODO conns przenieść do agenta i dostarczać tutaj przez konstruktor. 
		conns.put(name,  conn);
	}
	
	public String version() {
		return ZorkaBshAgent.VERSION;
	}
	
//	@ZoolaFunc(name="system")
//	public String system(String cmd) {
//		Process proc = null;
//		StringBuilder sb = new StringBuilder();
//		try {
//			proc = Runtime.getRuntime().exec(cmd);
//			BufferedReader rdr = new BufferedReader(
//				new InputStreamReader(proc.getInputStream()));
//		} 
//		catch (Exception e) { 
//			throw new ZoolaError("Failed executing command: " + cmd, e);
//		}
//		finally {
//			try { 
//				if (null != proc) proc.destroy();
//			} catch (Exception _) { }
//			try {
//				if (null != proc) proc.waitFor();
//			} catch (Exception _) { }
//		}
//		return sb.toString();
//	}
	
	
	public List<Object> jmxList(List<Object> args) {
		List<Object> objs = new ArrayList<Object>();
		if (args.size() < 2) {
			log.error("Zorka JMX function takes at least 2 arguments.");
			return objs;
		}
		MBeanServerConnection conn = conns.get(args.get(0));
		if (conn == null) {
			log.error("MBean server named '" + args.get(0) + "' is not registered.");
			return objs;
		}
		String conname = args.get(0).toString();
		Set<ObjectName> names = resolver.queryNames(conn, args.get(1).toString());
		if (args.size() == 2) {
			for (ObjectName name : names) {
				objs.add(new JmxObject(name, conn));
			}
		} else {
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
						obj = JmxResolver.get(obj, arg);
					}
				}
				objs.add(obj);
			}
		}
		return objs;
	} // jmxList()
	
	
	// TODO BSH fix badly needed
	public Object jmx(Object arg1) {
		return jmx(Arrays.asList(arg1));
	}
	
	
	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2) {
		return jmx(Arrays.asList(arg1,arg2));
	}
	
	
	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2, Object arg3) {
		return jmx(Arrays.asList(arg1,arg2,arg3));
	}
	
	
	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2, Object arg3, Object arg4) {
		return jmx(Arrays.asList(arg1,arg2,arg3,arg4));
	}
	
	
	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		return jmx(Arrays.asList(arg1,arg2,arg3,arg4,arg5));
	}
	
	
	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		return jmx(Arrays.asList(arg1,arg2,arg3,arg4,arg5,arg6));
	}

	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		return jmx(Arrays.asList(arg1,arg2,arg3,arg4,arg5,arg6,arg7));
	}

	// TODO BSH fix badly needed
	public Object jmx(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
		return jmx(Arrays.asList(arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8));
	}

	
	public Object jmx(List<Object> args) {
		
		if (args.size() < 2) {
			log.error("zorka.jmx() function requires at least 2 arguments");
			return null;
		}
		
		String conname = args.get(0).toString();
		MBeanServerConnection conn = conns.get(conname);
		
		if (conn == null) {
			log.error("MBean server named '" + args.get(0) + "' is not registered.");
			return null;
		}
		
		Set<ObjectName> names = resolver.queryNames(conn, args.get(1).toString());
		
		if (names.isEmpty()) { 
			return null;
		}
		
		ObjectName name = names.iterator().next();
		
		if (args.size() == 2) {
			return new JmxObject(name, conn);
		}
		
		Object obj = null;
		try {
			obj = conn.getAttribute(name, args.get(2).toString());
		} catch (AttributeNotFoundException e) {
			log.error("Object '" + conname + "|" + name + 
						"' has no attribute '" + args.get(2) + "'.");
			return null;
		} catch (Exception e) {
			log.error("Error getting attribute '" + args.get(2) 
				+ "' from '" + conname + "|" + name + "'", e);
		}
		
		if (args.size() > 3 && obj != null) {
			for (Object arg : args.subList(3, args.size())) {
				obj = JmxResolver.get(obj, arg);
			}
		}
		
		return obj;
	} // jmx()

	
	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1) {
		return getAttr(obj, arg1);
	}

	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1, Object arg2) {
		return getAttr(obj, arg1, arg2);
	}

	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1, Object arg2, Object arg3) {
		return getAttr(obj, arg1, arg2, arg3);
	}

	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1, Object arg2, Object arg3, Object arg4) {
		return getAttr(obj, arg1, arg2, arg3, arg4);
	}

	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		return getAttr(obj, arg1, arg2, arg3, arg4, arg5);
	}

	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		return getAttr(obj, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	// TODO BSH fix badly needed
	public Object get(Object obj, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		return getAttr(obj, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
	}

	
	private Object getAttr(Object obj, Object...args) {
		for (Object arg : args) {
			obj = JmxResolver.get(obj, arg);
		}
		return obj;
	}
	
	
	public ZorkaMappedMBean mbean(String mbs, String name) { 
		return mbean(mbs, name, "");
	}
	
	
	public ZorkaMappedMBean mbean(String mbs, String name, String desc) { 
		try {
			ZorkaMappedMBean mbean = new ZorkaMappedMBean(desc);
			MBeanServer conn = (MBeanServer)conns.get(mbs);
			if (conn == null) {
				throw new ZorkaException("There is no mbean server named '" + mbs + "'");
			}
			
			ObjectName on = new ObjectName(name);
			conn.registerMBean(mbean, on);
			registeredObjects.add(new JmxObject(on, conn));
			return mbean;
		} catch (Exception e) {
			// TODO zalogowac problem
			return null;
		}
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1) {
		return new AttrGetter(obj, attr1);
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1, String attr2) {
		return new AttrGetter(obj, attr1, attr2);
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1, String attr2, String attr3) {
		return new AttrGetter(obj, attr1, attr2, attr3);
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1, String attr2, String attr3, String attr4) {
		return new AttrGetter(obj, attr1, attr2, attr3, attr4);
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1, String attr2, String attr3, String attr4, String attr5) {
		return new AttrGetter(obj, attr1, attr2, attr3, attr4, attr5);
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1, String attr2, String attr3, String attr4, String attr5, String attr6) {
		return new AttrGetter(obj, attr1, attr2, attr3, attr4, attr5, attr6);
	}
	
	
	// TODO BSH fix badly needed
	public ValGetter getter(Object obj, String attr1, String attr2, String attr3, String attr4, String attr5, String attr6, String attr7) {
		return new AttrGetter(obj, attr1, attr2, attr3, attr4, attr5, attr6, attr7);
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
		MBeanServerConnection conn = conns.get(mbs);
		
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
}
