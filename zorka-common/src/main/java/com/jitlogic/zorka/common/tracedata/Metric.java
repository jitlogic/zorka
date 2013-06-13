/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.tracedata;

import com.jitlogic.zorka.common.util.ObjectInspector;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Metric implements Serializable {

    private transient MetricTemplate template;

    private int id, templateId;
    private String name;

    private Map<String,Object> attrs = new HashMap<String, Object>();

    /** Maps dynamic attributes to their symbol IDs */
    private Map<String,Integer> dynamicAttrs;

    public Metric(int id, String name, Map<String,Object> attrs) {
        this.id = id;
        this.name = name;
        this.attrs = attrs;
    }

    public Metric(int id, int templateId, String name, Map<String,Object> attrs) {
        this.id = id;
        this.templateId = templateId;
        this.name = name;
        this.attrs = attrs;
    }

    public Metric(MetricTemplate template, Set<Map.Entry<String,Object>> attrSet) {
        this.template = template;

        for (Map.Entry<String,Object> entry : attrSet) {
            this.attrs.put(entry.getKey(), entry.getValue().toString());
        }

        name = ObjectInspector.substitute(template.getName(), this.attrs);
    }


    public abstract Number getValue(long clock, Object value);


    public MetricTemplate getTemplate() {
        return template;
    }


    public void setTemplate(MetricTemplate template) {
        this.template = template;
    }


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public int getTemplateId() {
        return templateId;
    }


    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }


    public Map<String, Integer> getDynamicAttrs() {
        return dynamicAttrs;
    }


    public void setDynamicAttrs(Map<String, Integer> dynamicAttrs) {
        this.dynamicAttrs = dynamicAttrs;
    }


    public String getName() {
        return name;
    }


    public Map<String, Object> getAttrs() {
        return attrs;
    }

    protected Number multiply(Number value) {
        Double multiplier = getTemplate().getMultiplier();
        if (multiplier != 1.0) {
            if (value instanceof Double || Math.floor(multiplier) != multiplier) {
                return multiplier * value.doubleValue();
            } else {
                return multiplier.longValue() * value.longValue();
            }
        } else {
            return value;
        }
    }
}
