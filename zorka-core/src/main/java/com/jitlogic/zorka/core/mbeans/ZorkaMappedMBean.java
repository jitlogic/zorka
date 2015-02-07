/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.mbeans;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import com.jitlogic.zorka.common.stats.ValGetter;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;


/**
 * Zorka mapped (dynamic) mbean can be used to implement.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaMappedMBean implements DynamicMBean {

    /**
     * Logger
     */
    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaMappedMBean.class);

    /**
     * Bean description (presented by JMX)
     */
    private String description;

    /**
     * Attributes
     */
    private ConcurrentHashMap<String, Attribute> attrs = new ConcurrentHashMap<String, Attribute>();

    /**
     * If true, mbean info will be rebuilt at nearest occasion.
     */
    private AtomicInteger mbeanInfoChanges = new AtomicInteger(0);

    /**
     * MBean info
     */
    private volatile MBeanInfo mbeanInfo;

    /**
     * Creates mapped mbean
     *
     * @param description bean description
     */
    public ZorkaMappedMBean(String description) {
        this.description = description;
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException,
            MBeanException, ReflectionException {

        if (!attrs.containsKey(attribute)) {
            throw new AttributeNotFoundException("This MBean has no '" + attribute + "' attribute.");
        }

        // TODO ujawnic to w MBeanInfo
        if ("this".equals(attribute)) {
            return this;
        }

        Object v = attrs.get(attribute).getValue();

        return v instanceof ValGetter ? ((ValGetter) v).get() : v;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {

        synchronized (mbeanInfoChanges) {
            mbeanInfoChanges.incrementAndGet();
            attrs.put(attribute.getName(), attribute);
            refreshMBeanInfo();
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public AttributeList getAttributes(String[] attributes) {
        AttributeList lst = new AttributeList(attributes.length + 2);
        for (String attr : attributes) {
            try {
                final Object val = getAttribute(attr);
                lst.add(val);
            } catch (Exception e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error getting attribute '" + attr + "':", e);
            }
        }

        return lst;
    }


    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList lst = new AttributeList(attributes.size());
        for (Object attrObj : attributes) {
            Attribute attr = (Attribute) attrObj;
            try {
                setAttribute(attr);
                lst.add(new Attribute(attr.getName(), attr.getValue()));
            } catch (Exception e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error setting attribute '" + e + "'", e);
            }
        }

        return lst;
    }


    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {

        throw new MBeanException(new Exception("This object has no invokable methods."));
    }

    /**
     * Rebuilds mbean info
     */
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

            Object attrVal = attr.getValue();

            if (attrVal instanceof TypedValGetter) {
                String typeName = ((TypedValGetter) attrVal).getType().getTypeName();
                attrInfo[i] = new MBeanAttributeInfo(attrNames[i], typeName, attrNames[i], true, false, false);
            } else if (attrVal instanceof TabularDataGetter) {
                TabularDataGetter getter = (TabularDataGetter) attrVal;
                attrInfo[i] = new OpenMBeanAttributeInfoSupport(getter.getTypeName(), getter.getTypeDesc(),
                        getter.getTableType(), true, false, false);
            } else if (attrVal instanceof TabularData) {
                TabularType tt = ((TabularData) attrVal).getTabularType();
                attrInfo[i] = new OpenMBeanAttributeInfoSupport(tt.getTypeName(), tt.getDescription(),
                        tt, true, false, false);
            } else {
                attrInfo[i] = new MBeanAttributeInfo(attrNames[i],
                        attr.getValue() != null ? attr.getValue().getClass().getName() : "<null>",
                        attrNames[i], true, false, false);
            }
        }

        mbeanInfo = new MBeanInfo(
                this.getClass().getName(),
                description,
                attrInfo,
                new MBeanConstructorInfo[0],
                new MBeanOperationInfo[0],
                new MBeanNotificationInfo[0]);
    }  //


    @Override
    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    /**
     * Sets attribute value but behaves pretty much like
     * java.common.Map.put() method.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public void put(String name, Object value) {
        try {
            Attribute attr = new Attribute(name, value);
            setAttribute(attr);
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error setting attribute '" + name + "':", e);
        }
    }


    /**
     * Returns attribute value but behaves pretty much like
     * java.common.Map.get() method.
     *
     * @param name attribute name
     * @return attribute value
     */
    public Object get(String name) {
        Attribute attr = attrs.get(name);
        return attr != null ? attr.getValue() : null;
    }


    @Override
    public String toString() {
        return "ZorkaMappedMBean(" + description + ")";
    }

}
