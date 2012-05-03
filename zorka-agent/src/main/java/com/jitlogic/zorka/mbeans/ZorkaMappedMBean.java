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

package com.jitlogic.zorka.mbeans;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

// TODO proper structure for supporting parallelism (in somewhat more elegant way)

public class ZorkaMappedMBean implements DynamicMBean {

	private String description;
	private Map<String,Attribute> attrs = new HashMap<String, Attribute>();
	
	private boolean mbeanInfoChanged = true;
	private MBeanInfo mbeanInfo = null;
	
	public ZorkaMappedMBean(String description) {
		this.description = description;
	}
	
	public synchronized Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		if (!attrs.containsKey(attribute)) {
			throw new AttributeNotFoundException("This MBean has no '" + attribute + "' attribute.");
		}
		
		// TODO ujawnic to w MBeanInfo 
		if ("this".equals(attribute)) {
			return this;
		}
		
		Object v = attrs.get(attribute).getValue();
		
		return v instanceof ValGetter ? ((ValGetter)v).get() : v;
	}
	
	
	public synchronized void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		mbeanInfoChanged = true;
		attrs.put(attribute.getName(), attribute);
	}
	
	
	@SuppressWarnings("unchecked")
	public synchronized AttributeList getAttributes(String[] attributes) {
		AttributeList lst = new AttributeList(attributes.length);
		for (String attr : attributes) {
			try {
				final Object val = getAttribute(attr);
				lst.add(val);
			} catch (Exception e) {
				// Nothing to do here, move along !
			}
		}

		return lst;
	}
	
	
	public synchronized AttributeList setAttributes(AttributeList attributes) {
		AttributeList lst = new AttributeList(attributes.size());
		for (Object attrObj : attributes) {
			Attribute attr = (Attribute)attrObj;
			try {
				setAttribute(attr);
				lst.add(new Attribute(attr.getName(), attr.getValue()));
			} catch (Exception e) {
				// Nothing to do here, move along !
			}
		}

		return lst;
	}
	
	
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		// TODO setAttribute()/getAttribute() ?  
		throw new MBeanException(new Exception("This object has no invokable methods."));
	}
	
	
	private void refreshMBeanInfo() {
		int i = 0;
		
		String[] attrNames = new String[attrs.size()];
		
		for (String name : attrs.keySet()) {
			attrNames[i++] = name;
		}

		Arrays.sort(attrNames);

		MBeanAttributeInfo[] attrInfo = new MBeanAttributeInfo[attrNames.length];
		
		for (i = 0; i < attrNames.length; i++) {
			Attribute attr = attrs.get(attrNames[i]);
			attrInfo[i] = new MBeanAttributeInfo(attrNames[i], 
				attr.getValue() != null ? attr.getValue().getClass().getName() : "<null>", 
				attrNames[i], true, false, false);
		}
		
		mbeanInfo = new MBeanInfo(
				this.getClass().getName(), 
				description, 
				attrInfo, 
				new MBeanConstructorInfo[0], 
				new MBeanOperationInfo[0], 
				new MBeanNotificationInfo[0]);
		mbeanInfoChanged = false;
	}  // 
	
	
	public synchronized MBeanInfo getMBeanInfo() {
		if (mbeanInfoChanged)
			refreshMBeanInfo();
		return mbeanInfo;
	}

	public synchronized void put(String name, Object value) {
		try {
			Attribute attr = new Attribute(name, value);
			setAttribute(attr);
		} catch (Exception e) {
			// TODO obsluzyc i zalogowac problem
		}
	}
	
	public synchronized Object get(String name) {
		Attribute attr = attrs.get(name);
		return attr != null ? attr.getValue() : null;
	}
	
	public synchronized boolean hasAttribute(String name) {
		return attrs.containsKey(name);
	}
	
	@Override
	public String toString() {
		return "ZorkaMappedMBean(" + description + ")";
	}
	
}
