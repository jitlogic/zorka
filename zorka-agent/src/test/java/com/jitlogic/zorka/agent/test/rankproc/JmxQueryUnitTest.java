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

package com.jitlogic.zorka.agent.test.rankproc;

import com.jitlogic.zorka.agent.rankproc.JmxAttrScanner;
import com.jitlogic.zorka.agent.rankproc.QueryDef;
import com.jitlogic.zorka.agent.rankproc.QueryLister;
import com.jitlogic.zorka.agent.rankproc.QueryResult;
import com.jitlogic.zorka.agent.spy.Tracer;
import com.jitlogic.zorka.agent.test.spy.support.TestTracer;
import com.jitlogic.zorka.agent.test.support.TestJmx;
import com.jitlogic.zorka.agent.test.support.TestUtil;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;

import com.jitlogic.zorka.common.SymbolRegistry;
import com.jitlogic.zorka.common.ZorkaUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.List;
import java.util.Set;

public class JmxQueryUnitTest extends ZorkaFixture {

    private TestJmx jmx1;
    private TestJmx jmx2;

    @Before
    public void createSomeMBeans() throws Exception {
        jmx1 = makeTestJmx("test:name=bean1,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "uja", "wuja");
        jmx2 = makeTestJmx("test:name=bean2,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "eja", "weja");
    }


    @Test
    public void testTrivialSearchAttrOnly() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(2, results.size());

        Assert.assertEquals("bean1", results.get(0).getAttr("name"));
    }


    @Test
    public void testTrivialSearchAndGetSingleAttr() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("Nom", "Nom"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(2, results.size());

        Assert.assertEquals("Nom", results.get(0).getAttr("Nom"));
        Assert.assertEquals(10L, results.get(0).getValue());
    }


    @Test
    public void testSearchAndGetMultipleAttrs() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").list("*", "Attr"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(6, results.size());

        Set<String> attrs = ZorkaUtil.set("Nom", "Div", "StrMap");

        Assert.assertTrue(attrs.contains(results.get(0).getAttr("Attr")));
        Assert.assertTrue(attrs.contains(results.get(1).getAttr("Attr")));
    }


    @Test
    public void testSearchAndGetMultiSecondLevelAttr() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("StrMap").list("*", "Attr"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(6, results.size());

        Set<String> attrs = ZorkaUtil.set("oja", "aja", "uja", "eja");

        Assert.assertTrue(attrs.contains(results.get(0).getAttr("Attr")));
        Assert.assertTrue(attrs.contains(results.get(1).getAttr("Attr")));
    }


    @Test
    public void testSearchAndGetMultipleSecondLevelAttr() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("StrMap").get("oja", "Attr"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(2, results.size());

        Set<String> attrs = ZorkaUtil.set("oja", "aja", "uja", "eja");

        Assert.assertTrue(attrs.contains(results.get(0).getAttr("Attr")));
        Assert.assertTrue(attrs.contains(results.get(1).getAttr("Attr")));
    }


    @Test
    public void testJmxAttrScannerSimpleRun() throws Exception {
        TestTracer output = new TestTracer();
        JmxAttrScanner scanner = tracer.jmxScanner("TEST", output,
            new QueryLister(mBeanServerRegistry, new QueryDef("test", "test:type=TestJmx,*", "name").list("*", "Attr")));
        scanner.runCycle(100);
        Assert.assertEquals(1, output.size());
        int[] ids = (int[])output.get(0, "components");

        SymbolRegistry sr = ((Tracer)TestUtil.getField(tracer, "tracer")).getSymbolRegistry();

        Assert.assertEquals("TEST", sr.symbolName((Integer)output.get(0, "objId")));
        Assert.assertEquals("bean1.Nom", sr.symbolName(ids[0]));
        Assert.assertEquals("bean1.Div", sr.symbolName(ids[1]));
        Assert.assertEquals("bean2.Nom", sr.symbolName(ids[2]));
        Assert.assertEquals("bean2.Div", sr.symbolName(ids[3]));
    }


    private TestJmx makeTestJmx(String name, long nom, long div, String...md) throws Exception {
        TestJmx bean = new TestJmx();

        bean.setNom(nom); bean.setDiv(div);

        for (int i = 1; i < md.length; i += 2) {
            bean.put(md[i-1], md[i]);
        }

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }
}
