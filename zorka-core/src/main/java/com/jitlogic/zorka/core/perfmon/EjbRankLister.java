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
package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import javax.management.*;
import java.util.*;

/**
 * EJB Rank lister supplies EJB statistics to rank list objects.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class EjbRankLister implements Runnable, RankLister<EjbRankItem> {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(EjbRankLister.class);

    /** Tracked EJB statistics */
    private volatile Map<String,EjbRankItem> stats = new HashMap<String, EjbRankItem>();

    /** MBean server connection */
    private MBeanServerConnection mbs;

    /** Object name(s) that will be scanned for EJB statistics */
    private String objNames;

    /** Attribute name (typically 'stats') */
    private String attr;

    /**
     *
     * @param mbsName
     * @param objNames
     * @param attr
     */
    public EjbRankLister(MBeanServerRegistry registry, String mbsName, String objNames, String attr) {
        this.mbs = registry.lookup(mbsName);
        this.objNames = objNames;
        this.attr = attr;
    }


    @Override
    public List<EjbRankItem> list() {
        List<EjbRankItem> lst = new ArrayList<EjbRankItem>(stats.size()+1);

        for (Map.Entry<String,EjbRankItem> e : stats.entrySet()) {
            lst.add(e.getValue());
        }

        return lst;
    }

    /**
     * Performs discovery of new EJB statistics objects.
     */
    private void discovery() {
        Set<ObjectName> names = ObjectInspector.queryNames(mbs, objNames);

        for (ObjectName name : names) {
            try {
                Object statsObj = mbs.getAttribute(name, attr);
                for (Object s : ObjectInspector.list(statsObj)) {
                    String tag = name + "|" + attr + "|" + s;
                    if (!stats.containsKey(tag)) {
                        stats.put(tag, new EjbRankItem(ObjectInspector.get(statsObj, s)));
                    }
                }
            } catch (Exception e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Cannot perform discovery on " + name, e);
            }
        }

    }


    /**
     * Performs one discovery&update cycle
     *
     * @param tstamp current time
     */
    private void runCycle(long tstamp) {

        synchronized (this) {
            discovery();

            for (Map.Entry<String,EjbRankItem> e : stats.entrySet()) {
                e.getValue().feed(tstamp);
            }
        }
    }


    @Override
    public void run() {
        runCycle(System.currentTimeMillis());
    }

}
