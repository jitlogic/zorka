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

package com.jitlogic.zorka.core.test.util;

import com.jitlogic.zorka.core.util.ObjectInspector;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class ObjectInspectorUnitTest {


    private Properties props(String...kv) {
        Properties properties = new Properties();
        for (int i = 1; i < kv.length; i += 2) {
            properties.setProperty(kv[i-1], kv[i]);
        }
        return properties;
    }


    @Test
    public void testSubstituteWithPropertyValues() {
        Properties props = props("zorka.home.dir", "/opt/zorka", "my.prop", "Oja!");

        Assert.assertEquals("Oja! Zorka found in /opt/zorka !",
                ObjectInspector.substitute("${my.prop} Zorka found in ${zorka.home.dir} !", props));

        Assert.assertEquals("This is ${non.existent}.",
                ObjectInspector.substitute("This is ${non.existent}.", props));

        Assert.assertEquals("This is by default !",
                ObjectInspector.substitute("This is ${fubar:by default} !", props));
    }

    @Test
    public void testGetClassName() {
        Assert.assertEquals("java.lang.String", ObjectInspector.get(String.class, "name"));
    }

}
