/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.integ;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.agent.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.agent.perfmon.QueryDef;
import com.jitlogic.zorka.agent.perfmon.QueryLister;
import com.jitlogic.zorka.agent.perfmon.QueryResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Zabbix functions library.
 *
 * @author rafal.lewczuk@jitlogic.com
 *
 */
public class ZabbixLib {


    private Map<String,ZabbixTrapper> trappers = new ConcurrentHashMap<String, ZabbixTrapper>();


    /**
     * Zabbix discovery function using JMX query framework
     *
     * @param qdefs queries
     *
     * @return JSON object describing discovered objects
     */
    public JSONObject discovery(QueryDef...qdefs) {
        JSONArray data = new JSONArray();
        MBeanServerRegistry registry = AgentInstance.getMBeanServerRegistry();

        for (QueryDef qdef : qdefs) {
            for (QueryResult result : new QueryLister(registry, qdef).list()) {
                JSONObject item = new JSONObject();
                for (Map.Entry<String,Object> e : result.attrSet()) {
                    item.put("{#" + e.getKey().toUpperCase().replace("-", "") + "}", e.getValue().toString());
                }
                data.add(item);
            }
        }

        JSONObject discoveries = new JSONObject();
        discoveries.put("data", data);
        return discoveries;
    }

    /**
     * Simplified zabbix discovery function usable directly from zabbix.
     *
     *
     * @param mbs mbean server name
     *
     * @param filter object name filter
     *
     * @param attrs attribute chain
     *
     * @return JSON string describing discovered objects.
     */
    public JSONObject discovery(String mbs, String filter, String...attrs) {
        return discovery(new QueryDef(mbs, filter, attrs));
    }



    /**
     * Returns zabbix trapper registered as id or null.
     *
     * @param id trapper ID
     *
     * @return zabbix trapper or null
     */
    public ZabbixTrapper trapper(String id) {
        return trappers.get(id);
    }


    /**
     * Returns zabbix trapper or creates a new one (if not created already)
     * @param id trapper ID
     * @param serverAddr server address
     * @param defaultHost default host name
     * @return zabbix trapper
     */
    public ZabbixTrapper trapper(String id, String serverAddr, String defaultHost, String defaultItem) {
        ZabbixTrapper trapper = trappers.get(id);

        if (trapper == null) {
            trapper = new ZabbixTrapper(ZorkaConfig.propFormat(serverAddr),
                                        ZorkaConfig.propFormat(defaultHost), defaultItem);
            trappers.put(id, trapper);
            trapper.start();
        }

        return trapper;
    }


    /**
     * Stops and removes zabbix trapper
     * @param id trapper id
     */
    public void remove(String id) {
        ZabbixTrapper trapper = trappers.remove(id);

        if (trapper != null) {
            trapper.stop();
        }
    }
}
