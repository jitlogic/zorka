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

package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.common.test.support.TestJmx;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.perfmon.QueryLister;
import com.jitlogic.zorka.core.perfmon.QueryResult;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.List;
import java.util.Set;

public class JmxQueryUnitTest extends ZorkaFixture {

    @Before
    public void createSomeMBeans() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "uja", "wuja");
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "eja", "weja");
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
                new QueryDef("test", "test:type=TestJmx,*", "name").getAs("Nom", "Nom"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(2, results.size());

        Assert.assertEquals("Nom", results.get(0).getAttr("Nom"));
        Assert.assertEquals(10L, results.get(0).getValue());
    }


    @Test
    public void testSearchAndGetMultipleAttrs() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").listAs("*", "Attr"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(6, results.size());

        Set<String> attrs = ZorkaUtil.set("Nom", "Div", "StrMap");

        Assert.assertTrue(attrs.contains(results.get(0).getAttr("Attr")));
        Assert.assertTrue(attrs.contains(results.get(1).getAttr("Attr")));
    }


    @Test
    public void testSearchAndGetMultiSecondLevelAttr() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("StrMap").listAs("*", "Attr"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(6, results.size());

        Set<String> attrs = ZorkaUtil.set("oja", "aja", "uja", "eja");

        Assert.assertTrue(attrs.contains(results.get(0).getAttr("Attr")));
        Assert.assertTrue(attrs.contains(results.get(1).getAttr("Attr")));
    }


    @Test
    public void testSearchAndGetMultipleSecondLevelAttr() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("StrMap").getAs("oja", "Attr"));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(2, results.size());

        Set<String> attrs = ZorkaUtil.set("oja", "aja", "uja", "eja");

        Assert.assertTrue(attrs.contains(results.get(0).getAttr("Attr")));
        Assert.assertTrue(attrs.contains(results.get(1).getAttr("Attr")));
    }


    @Test
    public void testSearchWithSomeRecordsHavingNoSuchAttr() {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("StrMap").get("uja").with(QueryDef.NO_NULL_VALS));

        List<QueryResult> results = lister.list();

        Assert.assertEquals(1, results.size());
    }


    @Test
    public void testSearchWithNullAttrsInObjectName() throws Exception {
        QueryLister lister = new QueryLister(mBeanServerRegistry,
                new QueryDef("test", "test:*", "name").with(QueryDef.NO_NULL_ATTRS));
        makeTestJmx("test:name=oja", 10, 10);

        List<QueryResult> results = lister.list();

        Assert.assertEquals(1, results.size());
    }


    private TestJmx makeTestJmx(String name, long nom, long div, String... md) throws Exception {
        TestJmx bean = new TestJmx();

        bean.setNom(nom);
        bean.setDiv(div);

        for (int i = 1; i < md.length; i += 2) {
            bean.put(md[i - 1], md[i]);
        }

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }
}
