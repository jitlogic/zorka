/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.integ.zabbix;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.AgentConfigProps;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.perfmon.QueryLister;
import com.jitlogic.zorka.core.perfmon.QueryResult;
import com.jitlogic.zorka.common.util.JSONWriter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zabbix functions library.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZabbixLib implements ZorkaService {


    private Map<String, ZabbixTrapper> trappers = new ConcurrentHashMap<String, ZabbixTrapper>();

    private MBeanServerRegistry mbsRegistry;
    private ZorkaConfig config;

    /** Advertisement to be returned when zorka.req.prefix != null. */
    private String advertisement;

    /** Request prefix (if set in zabbix configuration) */
    private final String requestPrefix;

    private final String metadataTag;

    private static final String ZORKA = "{#ZORKA}";


    public ZabbixLib(MBeanServerRegistry mbsRegistry, ZorkaConfig config) {
        this.mbsRegistry = mbsRegistry;
        this.config = config;

        requestPrefix = config.stringCfg(AgentConfigProps.ZORKA_PREFIX_PROP, null);
        if (requestPrefix != null) {
            advertisement =  new JSONWriter().write(ZorkaUtil.map("data",
                    Collections.singletonList(ZorkaUtil.map(ZORKA, requestPrefix))));
        }
        metadataTag = requestPrefix != null ? "ZORKA:MULTI:" : "ZORKA:";
    }


    public String discovery(QueryDef... qdefs) {
        return discovery(QueryDef.NO_NULL_ATTRS, qdefs);
    }


    public Map<String, List<Map<String, String>>> _discovery(QueryDef... qdefs) {
        return _discovery(QueryDef.NO_NULL_ATTRS, qdefs);
    }

    /**
     * Zabbix discovery function using JMX query framework
     *
     * @param qdefs queries
     * @return JSON object describing discovered objects
     */
    public Map<String, List<Map<String, String>>> _discovery(int flags, QueryDef... qdefs) {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        for (QueryDef qdef : qdefs) {
            qdef = qdef.with(flags);
            for (QueryResult result : new QueryLister(mbsRegistry, qdef).list()) {
                Map<String, String> item = new HashMap<String, String>();
                for (Map.Entry<String, Object> e : result.attrSet()) {
                    if (e.getValue() == null) {
                        item = null;
                        break;
                    }
                    item.put("{#" + e.getKey().toUpperCase().replace("-", "") + "}", e.getValue().toString());
                    if (requestPrefix != null) item.put(ZORKA, requestPrefix);
                }
                if (item != null) {
                    data.add(item);
                }
            }
        }

        Map<String, List<Map<String, String>>> discoveries = new HashMap<String, List<Map<String, String>>>();
        discoveries.put("data", data);
        return discoveries;
    }


    public String discovery(int flags, QueryDef... qdefs) {
        return new JSONWriter().write(_discovery(flags, qdefs));
    }


    /**
     * Simplified zabbix discovery function usable directly from zabbix.
     *
     * @param mbs    mbean server name
     * @param filter object name filter
     * @param attrs  attribute chain
     * @return JSON string describing discovered objects.
     */
    public String discovery(String mbs, String filter, String... attrs) {
        return new JSONWriter().write(_discovery(mbs, filter, attrs));
    }


    public Map<String, List<Map<String, String>>> _discovery(String mbs, String filter, String... attrs) {
        return _discovery(QueryDef.NO_NULL_ATTRS, new QueryDef(mbs, filter, attrs));
    }

    public String advertise(String...args) {
        return advertisement;
    }

    /**
     * Returns zabbix trapper registered as id or null.
     *
     * @param id trapper ID
     * @return zabbix trapper or null
     */
    public ZabbixTrapper trapper(String id) {
        return trappers.get(id);
    }


    /**
     * Returns zabbix trapper or creates a new one (if not created already)
     *
     * @param id          trapper ID
     * @param serverAddr  server address
     * @param defaultHost default host name
     * @return zabbix trapper
     */
    public ZabbixTrapper trapper(String id, String serverAddr, String defaultHost, String defaultItem) {
        ZabbixTrapper trapper = trappers.get(id);

        if (trapper == null) {
            trapper = new ZabbixTrapper(config.formatCfg(serverAddr),
                    config.formatCfg(defaultHost), defaultItem);
            trappers.put(id, trapper);
            trapper.start();
        }

        return trapper;
    }


    public void tagMetadata(String tag) {
        String meta = config.stringCfg("zabbix.active.metadata", "");

        // TODO always sort tags (?)

        if (!tag.startsWith(metadataTag)) {
            tag = metadataTag + tag;
        }

        if (!meta.contains(tag)) {
            config.setCfg("zabbix.active.metadata", meta + " " + tag);
        }
    }


    /**
     * Stops and removes zabbix trapper
     *
     * @param id trapper id
     */
    public void remove(String id) {
        ZabbixTrapper trapper = trappers.remove(id);

        if (trapper != null) {
            trapper.shutdown();
        }
    }

    @Override
    public void shutdown() {
        for (ZabbixTrapper trapper : trappers.values()) {
            trapper.shutdown();
        }
    }
}
