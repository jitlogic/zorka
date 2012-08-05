package com.jitlogic.zorka.agent.zabbix;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.management.ObjectName;
import java.util.Arrays;
import java.util.List;

/**
 *
 * This library module
 *
 * @author RLE <rle@jitlogic.com>
 *
 */
public class ZabbixLib {

    private ZorkaBshAgent bshAgent;
    private ZorkaLib zorkaLib;

    public ZabbixLib(ZorkaBshAgent bshAgent, ZorkaLib zorkaLib) {
        this.bshAgent = bshAgent;
        this.zorkaLib = zorkaLib;
    }

    /**
     *
     * @param mbs
     * @param filter
     * @param attrs
     *
     * @return
     */
    public JSONObject discovery(String mbs, String filter, String...attrs) {

        List<Object> objs = zorkaLib.jmxList(Arrays.asList((Object)mbs, filter));

        JSONArray dlist = new JSONArray();

        for (Object obj : objs) {
            if (obj instanceof JmxObject) {
                ObjectName on = ((JmxObject)obj).getName();
                JSONObject odo = new JSONObject();
                for (String attr : attrs) {
                    String atval = on.getKeyProperty(attr);
                    if (atval != null) {
                        odo.put("{#" + attr.toUpperCase() + "}", atval);
                    }
                }
                if (odo.size() > 0) {
                    dlist.add(odo);
                }
            }
        }

        JSONObject discoveries = new JSONObject();
        discoveries.put("data", dlist);

        return discoveries;
    }

}
