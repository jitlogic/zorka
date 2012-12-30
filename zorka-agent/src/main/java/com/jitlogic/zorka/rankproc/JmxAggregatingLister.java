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
import java.util.ArrayList;
import java.util.List;

public class JmxAggregatingLister<T extends Rankable<?>> implements RankLister<T> {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private ClassLoader classLoader;
    private MBeanServerConnection mbsConn;
    private String onMask;


    public JmxAggregatingLister(String mbsName, String onMask) {
        this.mbsConn = AgentInstance.getMBeanServerRegistry().lookup(mbsName);
        this.classLoader = AgentInstance.getMBeanServerRegistry().getClassLoader(mbsName);
        this.onMask = onMask;
    }


    public List<T> list() {

        List<T> lst = new ArrayList<T>();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        for (ObjectName on : ObjectInspector.queryNames(mbsConn, onMask)) {
            try {
                MBeanInfo mbi = mbsConn.getMBeanInfo(on);
                for (MBeanAttributeInfo mba : mbi.getAttributes()) {
                    Object obj = mbsConn.getAttribute(on, mba.getName());
                    if (obj instanceof Rankable) {
                        lst.add((T)obj);
                    } else if (obj instanceof RankLister) {
                        lst.addAll((List<T>)((RankLister)obj).list());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing name '" + on + "'", e);
            }
        }

        Thread.currentThread().setContextClassLoader(cl);

        return lst;
    }

}
