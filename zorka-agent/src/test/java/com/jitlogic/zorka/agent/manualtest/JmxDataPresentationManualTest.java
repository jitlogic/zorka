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

package com.jitlogic.zorka.agent.manualtest;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.mbeans.TabularDataGetter;
import com.jitlogic.zorka.mbeans.TabularDataWrapper;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmxDataPresentationManualTest extends ZorkaFixture {

    List<?> listOfMaps = Arrays.asList(
            map("name", "aaa", "type", "ttt", "val", 1),
            map("name", "bbb", "type", "ttt", "val", 2)
    );

    private Map map(Object...vals) {
        HashMap<String,Object> ret = new HashMap<String, Object>(vals.length);

        for (int i = 0; i < vals.length; i+=2)
            ret.put(vals[i].toString(), vals[i+1]);

        return ret;
    }

    //@Test
    public void testWrapMapOfMapsAndPresentAsAnMBean() throws Exception {
        TabularDataWrapper<Map> tdw = new TabularDataWrapper<Map>(Map.class, listOfMaps, "asd", "name",
                new String[] { "name", "type", "val" },
                new OpenType[] {SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER  } );

        mBeanServerRegistry.getOrRegister("java", "zorka:name=TabularWrapperTest", "table", tdw, "Test table");

        while (true) Thread.sleep(1000);
    }

    //@Test
    public void testPresentTabularDataGetter() throws Exception {
        TabularDataGetter getter = new TabularDataGetter(listOfMaps, "test", "test", "name",
                new String[] { "name", "type", "val" }, new String[] { "name", "type", "val" },
                new OpenType[] {SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER });

        mBeanServerRegistry.getOrRegister("java", "zorka:name=TabularTest1", "table", getter, "Test table");

        while (true) Thread.sleep(1000);
    }
}
