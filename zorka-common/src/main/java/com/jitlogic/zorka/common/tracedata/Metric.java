/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents performance metric tracked by agent. Metrics are created from metric templates
 * based on performance data object scanned by .
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public abstract class Metric implements Serializable {

    /**
     * Template this metric was created from
     */
    private transient MetricTemplate template;

    /**
     * Metric ID and tempalte ID
     */
    private int id, templateId;

    /**
     * Metric short name (eg. cpu_load etc.)
     */
    private String name;

    /**
     * Metric description
     */
    private String description;

    /**
     * Dynamic attributes for this metric
     */
    private Map<String, Object> attrs = new HashMap<String, Object>();

    /**
     * Maps dynamic attributes to their symbol IDs
     */
    private Map<String, Integer> dynamicAttrs;


    /**
     * Creates new metric
     *
     * @param id    metric ID
     * @param name metric short name
     * @param description  metric description
     * @param attrs metric attributes
     */
    public Metric(int id, String name, String description, Map<String, Object> attrs) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attrs = attrs;
    }


    /**
     * Creates new metric
     *
     * @param id         metric ID
     * @param templateId template ID
     * @param description       metric description
     * @param attrs      metric attributes
     */
    public Metric(int id, int templateId, String name, String description, Map<String, Object> attrs) {
        this.id = id;
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.attrs = attrs;
    }

    /**
     * Creates new metric
     *
     * @param template metric template
     * @param description     metric description
     * @param attrs    metric attributes
     */
    public Metric(MetricTemplate template, String name, String description, Map<String, Object> attrs) {
        this.template = template;

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            this.attrs.put(entry.getKey(), entry.getValue().toString());
        }

        this.name = name;
        this.description = description;
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


    public String getDescription() {
        return description;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public Map<String, Object> getAttrs() {
        return attrs;
    }


    /**
     * Multiplies value using multiplier from templates and automatic type coercion.
     *
     * @param value value to be multiplied
     * @return multiplied value
     */
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
