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


import javax.management.Attribute;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.mbeans.MethodCallStatisticImpl;
import com.jitlogic.zorka.mbeans.MethodCallStats;
import com.jitlogic.zorka.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

public class ZorkaSpyLib {
	
	
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	
	//private ZorkaBshAgent agent;
	private ZorkaSpy spy;
	private ZorkaLib lib;
	
	
	public ZorkaSpyLib(ZorkaBshAgent agent) {
		//this.agent = agent;
		this.lib = agent.getZorkaLib();
		spy = new ZorkaSpy();
	}
	
	
	private MethodCallStats getStats(String beanName, String attrName) {
		Object obj = lib.jmx("java", beanName, attrName);
		
		if (obj == null) {
			obj = new MethodCallStats();
			JmxObject jmxobj = (JmxObject)lib.jmx("java", beanName);
			try {
				if (jmxobj != null) {
					jmxobj.getConn().setAttribute(jmxobj.getName(), new Attribute(attrName, obj));
				} else {
					ZorkaMappedMBean bean = lib.mbean("java", beanName);
					bean.setAttribute(new Attribute(attrName, obj));
				}
			} catch (Exception e) {
				log.error("Error setting attribute '" + attrName + "' for '" + beanName, e);
				return null;
			}
		}
		
		if (! (obj instanceof MethodCallStats)) {
			log.error("Attribute '" + attrName + "' of '" + beanName + "' is not MethodCallStats object.");
		}
		
		return (MethodCallStats)obj;
	}

	
	public void simple(String className, String methodName, String beanName, String attrName) {
		MethodCallStats mcs = getStats(beanName, attrName);
		MethodCallStatisticImpl st = mcs.getMethodCallStat(methodName);
		DataCollector collector = new SingleMethodDataCollector(st);
		MethodTemplate mt = new MethodTemplate(className, methodName, null, collector);
		spy.addTemplate(mt);
	}
		
	
	public void simple(String className, String methodName, String beanName, String attrName, String expr) {
		MethodCallStats mcs = getStats(beanName, attrName);
		DataCollector collector = new MultiMethodDataCollector(mcs, SpyExpression.parse(expr));
		MethodTemplate mt = new MethodTemplate(className, methodName, null, collector);
		spy.addTemplate(mt);
	}
	
	
	public ZorkaSpy getSpy() {
		return spy;
	}
}
