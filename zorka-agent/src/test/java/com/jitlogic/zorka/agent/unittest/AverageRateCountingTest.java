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

package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.*;
import com.jitlogic.zorka.agent.testutil.*;
import com.jitlogic.zorka.rankproc.AvgRateCounter;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.zabbix.ZabbixLib;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;

import static org.junit.Assert.*;

import java.util.List;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class AverageRateCountingTest extends ZorkaFixture {

    private AvgRateCounter counter;

    @Before
    public void setUp() {
        counter = new AvgRateCounter(zorkaAgent.getZorkaLib());
    }

    @Test
    public void testRegisterAndQueryTrivialBean() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        assertEquals("10", zorkaAgent.query("zorka.jmx(\"test\", \"test:name=bean1,type=TestJmx\", \"Nom\")"));
    }

    @Test
    public void testOneObjectAvgRateCount() throws Exception {
        TestJmx tj = makeTestJmx("test:name=bean1,type=TestJmx", 0, 0);
        List<Object> path = counter.list("test", "test:name=bean1,type=TestJmx");

        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);
        tj.setNom(5); tj.setDiv(5);

        assertEquals(1.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);
        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.5, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);
    }

    @Test
    public void testOneObjectTwoAverages() throws Exception {
        TestJmx tj = makeTestJmx("test:name=bean1,type=TestJmx", 0, 0);
        List<Object> path = counter.list("test", "test:name=bean1,type=TestJmx");

        tj.setNom(5); tj.setDiv(5);
        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);

        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG5), 0.01);

        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.33, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);

        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG5), 0.01);
    }

    @Test
    public void testZorkaLibRateFn1() throws Exception {
        TestJmx tj = makeTestJmx("test:name=bean1,type=TestJmx", 0, 0);

        assertEquals(0.0, zorkaLib.rate("test", "test:name=bean1,type=TestJmx", "Nom", "Div", 60), 0.01);
        tj.setNom(5); tj.setDiv(5);
        assertEquals(1.0, zorkaLib.rate("test", "test:name=bean1,type=TestJmx", "Nom", "Div", 60), 0.01);
        assertEquals(1.0, zorkaLib.rate("test", "test:name=bean1,type=TestJmx", "Nom", "Div", "AVG1"), 0.01);
    }

    private TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom); bean.setDiv(div);

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }


}
