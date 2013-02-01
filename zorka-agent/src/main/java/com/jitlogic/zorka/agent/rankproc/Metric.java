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

package com.jitlogic.zorka.agent.rankproc;

import com.jitlogic.zorka.common.ObjectInspector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Metric {

    private MetricTemplate template;

    private int id;
    private String name;

    private Map<String,Object> attrs = new HashMap<String, Object>();


    public Metric(MetricTemplate template, Set<Map.Entry<String,Object>> attrSet) {
        this.template = template;

        for (Map.Entry<String,Object> entry : attrSet) {
            this.attrs.put(entry.getKey(), entry.getValue().toString());
        }

        name = ObjectInspector.substitute(template.getName(), this.attrs);
    }


    public abstract Number getValue(long clock, QueryResult result);


    public MetricTemplate getTemplate() {
        return template;
    }


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }


    public Map<String, Object> getAttrs() {
        return attrs;
    }
}
