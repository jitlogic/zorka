package com.jitlogic.zorka.integ;

import com.jitlogic.zorka.util.JmxObject;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.util.ObjectInspector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private ZorkaBshAgent bshAgent;
    private ZorkaLib zorkaLib;

    private Map<String,ZabbixTrapper> trappers = new ConcurrentHashMap<String, ZabbixTrapper>();

    /**
     * Creates  new zabbix library module
     *
     * @param bshAgent zorka BSH agents
     *
     * @param zorkaLib zorka library
     */
    public ZabbixLib(ZorkaBshAgent bshAgent, ZorkaLib zorkaLib) {
        this.bshAgent = bshAgent;
        this.zorkaLib = zorkaLib;
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
        return discovery(mbs,filter, attrs, new String[0], new String[0]);
    }


    /**
     * Full-fledged zabbix discovery function.
     *
     * @param mbs mbean server connection name
     *
     * @param filter object name filter
     *
     * @param oattrs object name attributes to be included in discovery result
     *
     * @param attrs attribute chain to interesting attribute (can be
     *
     * @param pattrs
     * @return
     */
    public JSONObject discovery(String mbs, String filter, String[] oattrs, String[] attrs, String[] pattrs) {

        List<Object> osrc = zorkaLib.jmxList(Arrays.asList((Object)mbs, filter));
        JSONArray    dsrc = new JSONArray();

        // List objects from mbean server
        for (Object obj : osrc) {
            if (obj instanceof JmxObject) {
                ObjectName on = ((JmxObject)obj).getName();
                JSONObject odo = new JSONObject();
                for (String attr : oattrs) {
                    String atval = on.getKeyProperty(attr);
                    if (atval != null) {
                        odo.put("{#" + attr.toUpperCase().replace("-", "") + "}", atval);
                    } else {
                        // A bit of a hack - filter out all objects without all (queried) attributes
                        odo.clear();
                        break;
                    }
                }
                if (odo.size() > 0) {
                    dsrc.add(odo);
                } else {
                    dsrc.add(null);
                }
            } else {
                dsrc.add(null);
            }
        }

        // Iterate over fetched list, resolve path and add attributes
        for (int pidx = 0; pidx < attrs.length; pidx++) {
            List<Object> odst = new ArrayList<Object>(osrc.size()+2);
            JSONArray ddst = new JSONArray();

            String pathItem = attrs[pidx], pathAttr = pattrs[pidx];

            for (int oidx = 0; oidx < osrc.size(); oidx++) {

                Object srcObj = osrc.get(oidx);
                JSONObject dstObj = (JSONObject)dsrc.get(oidx);

                if (dstObj == null) continue;

                if (pathItem.startsWith("~")) {
                    for (Object attr : ObjectInspector.list(srcObj)) {
                        if (attr != null && attr.toString().matches(pathItem.substring(1))) {
                            Object obj = ObjectInspector.get(srcObj, attr);
                            if (obj != null) {
                                JSONObject dsr = pathAttr == null ? dstObj : extend(dstObj, pathAttr, attr.toString());
                                odst.add(obj);
                                ddst.add(dsr);
                            }
                        }
                    }
                } else {
                    Object obj = ObjectInspector.get(srcObj, pathItem);
                    if (obj != null) {
                        JSONObject dsr = pathAttr == null ? dstObj : extend(dstObj, pathAttr, pathItem);
                        odst.add(obj);
                        ddst.add(dsr);
                    }
                } //
            } // for (int oidx = 0 ...
            osrc = odst;
            dsrc = ddst;
        } // for (int pidx = 0 ...

        JSONObject discoveries = new JSONObject();
        JSONArray data = new JSONArray();

        for (Object o : dsrc) {
            if (o != null) data.add(o);
        }

        discoveries.put("data", data);

        return discoveries;
    } // discovery()


    /**
     * Adds key-value pair to JSON object
     *
     * @param src existing JSON object
     *
     * @param key key
     *
     * @param val value
     *
     * @return extended JSON object
     */
    private JSONObject extend(JSONObject src, String key, String val) {
        JSONObject obj = new JSONObject();
        obj.putAll(src);

        obj.put("{#" + key.toUpperCase().replace("-", "") + "}", val);

        return obj;
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
    public ZabbixTrapper trapper(String id, String serverAddr, String defaultHost) {
        ZabbixTrapper trapper = trappers.get(id);

        if (trapper == null) {
            trapper = new ZabbixTrapper(serverAddr, defaultHost, "traps");
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
