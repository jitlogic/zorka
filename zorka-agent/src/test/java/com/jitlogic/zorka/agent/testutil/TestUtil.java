/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.testutil;

import com.jitlogic.zorka.agent.JavaAgent;
import com.jitlogic.zorka.agent.ZorkaBshAgent;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TestUtil {

    private MBeanServer mbs;
    private ZorkaBshAgent agent;


    public void setUp(ZorkaBshAgent agent) {
        mbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        this.agent = agent;
        this.agent.getMBeanServerRegistry().register("test", mbs, null);
    }


    public void tearDown() {
        agent.getMBeanServerRegistry().unregister("test");
    }


    public TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom); bean.setDiv(div);

        mbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

}
