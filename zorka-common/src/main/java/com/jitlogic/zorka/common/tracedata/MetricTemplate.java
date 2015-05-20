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

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.Serializable;
import java.util.*;

/**
 * Metric template. This is used to create new metrics for tracking
 * performance data inside VM.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MetricTemplate implements Serializable {

    public static final int RAW_DATA = 1;
    public static final int RAW_DELTA = 2;
    public static final int TIMED_DELTA = 3;
    public static final int WINDOWED_RATE = 4;
    public static final int UTILIZATION = 5;

    /**
     * Template ID (automatically assigned by agent)
     */
    private int id;

    /**
     * Determines types of metrics created from this template.
     */
    private int type;

    /**
     * Metric short name (eg. cpu_load, mem_util etc.)
     */
    private String name;

    /**
     * Metric description
     */
    private String description;

    /**
     * Units of measure (human readable string)
     */
    private String units;

    /**
     * Nominator and divider fields (for metrics using two components)
     */
    private String nomField, divField;

    /**
     * Result multiplier
     */
    private double multiplier = 1.0;

    /**
     * Names of dynamic attributes
     */
    private Set<String> dynamicAttrs = new HashSet<String>();

    /**
     * Metrics created from this template so far
     */
    private Map<String, Metric> metrics = new HashMap<String, Metric>();


    /**
     * Creates new template
     *
     * @param type  metric type
     * @param description  metric description
     * @param units units of measure
     */
    public MetricTemplate(int type, String name, String description, String units) {
        this(type, name, description, units, null, null);
    }


    /**
     * Creates new template
     *
     * @param id       template ID
     * @param type     metric type
     * @param description     metric description
     * @param units    units of measure
     * @param nomField nominal field
     * @param divField divider field
     */
    public MetricTemplate(int id, int type, String name, String description, String units, String nomField, String divField) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.units = units;
        this.nomField = nomField;
        this.divField = divField;
    }


    /**
     * Creates new template
     *
     * @param type     metric type
     * @param description     metric description
     * @param units    units of measure
     * @param nomField nominal field
     * @param divField divider field
     */
    public MetricTemplate(int type, String name, String description, String units, String nomField, String divField) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.units = units;
        this.nomField = nomField;
        this.divField = divField;
    }


    /**
     * Creates copy of metric template
     *
     * @param orig original template
     */
    private MetricTemplate(MetricTemplate orig) {
        this.id = orig.id;
        this.type = orig.type;
        this.name = orig.name;
        this.description = orig.description;
        this.nomField = orig.nomField;
        this.divField = orig.divField;
        this.multiplier = orig.getMultiplier();
        this.dynamicAttrs = new HashSet<String>();
        this.dynamicAttrs.addAll(orig.getDynamicAttrs());
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricTemplate) {
            MetricTemplate mt = (MetricTemplate) obj;
            return type == mt.type
                    && ZorkaUtil.objEquals(description, mt.description)
                    && ZorkaUtil.objEquals(nomField, mt.nomField)
                    && ZorkaUtil.objEquals(divField, mt.divField);
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return (1117 * type) ^ (description != null ? description.hashCode() : 0);
    }


    @Override
    public String toString() {
        return "MT(type=" + type + ", description=" + description + ")";
    }


    public int getType() {
        return type;
    }


    public String getDescription() {
        return description;
    }


    public String getUnits() {
        return units;
    }


    public String getNomField() {
        return nomField;
    }


    public String getDivField() {
        return divField;
    }


    public double getMultiplier() {
        return multiplier;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    /**
     * Changes multiplier ratio by multiplying it with passed argument.
     * Returns new version of metric template object, original object is unchanged.
     *
     * @param multiplier new multiplier
     * @return altered metric template
     */
    public MetricTemplate multiply(double multiplier) {
        MetricTemplate mt = new MetricTemplate(this);
        mt.multiplier = mt.multiplier * multiplier;
        return mt;
    }


    /**
     * Changes multiplier ratio by dividing it with passed argument.
     * Returns new version of metric template object, original object is unchanged.
     *
     * @param divider new inverted multiplier
     * @return altered metric template
     */
    public MetricTemplate divide(double divider) {
        MetricTemplate mt = new MetricTemplate(this);
        mt.multiplier = mt.multiplier / multiplier;
        return mt;
    }


    public Set<String> getDynamicAttrs() {
        return dynamicAttrs;
    }


    /**
     * Adds new dynamic attributes to metric template
     * Returns new version of metric template object, original object is unchanged.
     *
     * @param attrs new dynamic attribute names
     * @return altered metric template
     */
    public MetricTemplate dynamicAttrs(String... attrs) {
        MetricTemplate mt = new MetricTemplate(this);
        for (String attr : attrs) {
            mt.dynamicAttrs.add(attr);
        }
        return mt;
    }


    /**
     * Adds new dynamic attributes to metric template
     * Returns new version of metric template object, original object is unchanged.
     *
     * @param attrs new dynamic attribute names
     * @return altered metric template
     */
    public MetricTemplate dynamicAttrs(Collection<String> attrs) {
        if (attrs == null) {
            return this;
        }
        MetricTemplate mt = new MetricTemplate(this);
        mt.dynamicAttrs.addAll(attrs);
        return mt;
    }


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public Metric getMetric(String key) {
        return metrics.get(key);
    }


    public void putMetric(String key, Metric metric) {
        metrics.put(key, metric);
    }

}
