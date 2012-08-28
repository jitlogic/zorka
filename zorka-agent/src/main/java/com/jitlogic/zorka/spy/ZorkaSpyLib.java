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

package com.jitlogic.zorka.spy;


import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.mbeans.MethodCallStatisticImpl;
import com.jitlogic.zorka.mbeans.MethodCallStats;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

public class ZorkaSpyLib {
	
	
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	
	//private ZorkaBshAgent agent;
	private ZorkaSpy spy;
	//private ZorkaLib lib;
    private MBeanServerRegistry mbr;
	
	
	public ZorkaSpyLib(ZorkaBshAgent agent) {
		//this.agent = agent;
		//this.lib = agent.getZorkaLib();
		spy = new ZorkaSpy();
        mbr = agent.getMBeanServerRegistry();
	}
	
	
//	private MethodCallStats getStats(String bean, String attrName) {
//		Object obj = mbr.getOrRegisterBeanAttr("java", bean, attrName, new MethodCallStats());
//
//		if (obj == null) {
//			obj = new MethodCallStats();
//			JmxObject jmxobj = (JmxObject)lib.jmx("java", bean);
//			try {
//				if (jmxobj != null) {
//					jmxobj.getConn().setAttribute(jmxobj.getName(), new Attribute(attrName, obj));
//				} else {
//					ZorkaMappedMBean bean = lib.mbean("java", bean);
//					bean.setAttribute(new Attribute(attrName, obj));
//				}
//			} catch (Exception e) {
//				log.error("Error setting attribute '" + attrName + "' for '" + bean, e);
//				return null;
//			}
//		}
//
//		if (! (obj instanceof MethodCallStats)) {
//			log.error("Attribute '" + attrName + "' of '" + bean + "' is not MethodCallStats object.");
//		}
//
//		return (MethodCallStats)obj;
//	}

	
	public void simple(String className, String methodName, String beanName, String attrName) {

		MethodCallStats mcs = mbr.getOrRegisterBeanAttr("java", beanName, attrName, new MethodCallStats(),
                                "Zorka Spy Stats"); //getStats(bean, attrName);
		MethodCallStatisticImpl st = mcs.getMethodCallStat(methodName);
		DataCollector collector = new SingleMethodDataCollector(st);
		MethodTemplate mt = new MethodTemplate(className, methodName, null, collector);
		spy.addTemplate(mt);

	}
		
	
	public void simple(String className, String methodName, String beanName, String attrName, String expr) {

		MethodCallStats mcs = mbr.getOrRegisterBeanAttr("java", beanName, attrName, new MethodCallStats(),
                                "Zorka Spy stats"); //getStats(bean, attrName);
		DataCollector collector = new MultiMethodDataCollector(mcs, SpyExpression.parse(expr));
		MethodTemplate mt = new MethodTemplate(className, methodName, null, collector);
		spy.addTemplate(mt);

	}


    public static final int PRESENT_ARGUMENT = 1;
    public static final int PRESENT_ATTRIBUTE = 2;
    public static final int PRESENT_STATIC = 3;


    /**
     * Catches specified method call of a specified class and presents some object visible at beginning of the method
     * as an attribute in a JMX bean.
     *
     * @param className
     * @param methodName
     * @param beanName
     * @param attrName
     * @param argPath
     * @param getPath
     */
    public void present(String mbeanServer, String className, String methodName,
                        String beanName, String attrName,
                        Object argObj, String[] argPath, String[] getPath,
                        int type, boolean once) {

        DataCollector collector = new PresentingDataCollector(mbeanServer, beanName, attrName,
                                                    argObj, argPath, getPath, type, once);

        MethodTemplate mt = new MethodTemplate(className, methodName, null, collector);
        spy.addTemplate(mt);
    }

    // TODO function similiar to present() that will pass extracted argument to a user-specified BSH function

    public ZorkaSpy getSpy() {
		return spy;
	}
}
