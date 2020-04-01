/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

    /**
     * Template ID (automatically assigned by agent)
     */
    private int id;

    /**
     * Domain name (as in JMX).
     */
    private String domain;

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
     * Data type (counter, gauge, histogram, summary)
     */
    private String type;

    /**
     * Result multiplier
     */
    private double multiplier = 1.0;

    /**
     * Metrics created from this template so far
     */
    private Map<String, Metric> metrics = new HashMap<String, Metric>();


    /**
     * Creates new template
     *  @param description  metric description
     * @param units units of measure
     */
    public MetricTemplate(String domain, String name, String description, String units, String type) {
        this.name = name;
        this.domain = domain;
        this.description = description;
        this.units = units;
        this.type = type;
    }


    /**
     * Creates copy of metric template
     *
     * @param orig original template
     */
    private MetricTemplate(MetricTemplate orig) {
        this.id = orig.id;
        this.domain = orig.domain;
        this.name = orig.name;
        this.description = orig.description;
        this.units = orig.units;
        this.type = orig.type;
        this.multiplier = orig.multiplier;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricTemplate) {
            MetricTemplate mt = (MetricTemplate) obj;
            return ZorkaUtil.objEquals(name, mt.name)
                && ZorkaUtil.objEquals(domain, mt.domain)
                && ZorkaUtil.objEquals(units, mt.units)
                && ZorkaUtil.objEquals(type, mt.getType())
                && ZorkaUtil.objEquals(description, mt.description);
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return name.hashCode() + (1117 * type.hashCode()) ^ (description != null ? description.hashCode() : 0);
    }


    @Override
    public String toString() {
        return "MT(name=" + name + ", description=" + description + ")";
    }


    public String getDomain() {
        return domain;
    }

    public String getDescription() {
        return description;
    }


    public String getUnits() {
        return units;
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

    public String getType() {
        return type;
    }
}
