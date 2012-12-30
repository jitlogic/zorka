/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.rankproc;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;

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

    /** Data collection interval */
    private long interval;

    private volatile boolean started = false;
    private volatile Thread thread = null;

    private Map<String,EjbRankItem> stats = new HashMap<String, EjbRankItem>();

    /** MBean server connection */
    private MBeanServerConnection mbs;


    private String objNames, attr;

    public EjbRankLister(String mbsName, String objNames, String attr) {
        this.mbs = AgentInstance.getMBeanServerRegistry().lookup(mbsName);
        this.objNames = objNames;
        this.attr = attr;
        this.interval = 14000;
    }

    @Override
    public synchronized List<EjbRankItem> list() {
        List<EjbRankItem> lst = new ArrayList<EjbRankItem>(stats.size()+1);

        for (Map.Entry<String,EjbRankItem> e : stats.entrySet()) {
            lst.add(e.getValue());
        }

        return lst;
    }

    /**
     * Performs discovery of new EJB statistics objects.
     */
    public synchronized void discovery() {
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
                log.error("Cannot perform discovery on " + name, e);
            }
        }

    }


    private void runCycle(long tstamp) {
        discovery();

        synchronized (this) {
            for (Map.Entry<String,EjbRankItem> e : stats.entrySet()) {
                e.getValue().feed(tstamp);
            }
        }
    }


    public void run() {
        while (started) {
            runCycle(System.currentTimeMillis());
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) { }

        }

        thread = null;
    }

    public synchronized void start() {
        if (!started) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName("Zorka-thread-lister");
            thread.start();
        }
    }


    public synchronized void stop() {
        if (started) {
            thread.interrupt();
            started = false;
        }
    }

}
