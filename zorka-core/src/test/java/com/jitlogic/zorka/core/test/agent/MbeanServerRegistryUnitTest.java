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

package com.jitlogic.zorka.core.test.agent;

import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

public class MbeanServerRegistryUnitTest  extends ZorkaFixture {

    @Test
    public void testRegisterObjectInNotYetExistentMbsTwice() throws Exception {
        Object obj1 = new Object(), obj2 = new Object();

        assertSame(obj1, mBeanServerRegistry.getOrRegister("xxx", "test:name=Test", "stats", obj1));
        assertSame(obj1, mBeanServerRegistry.getOrRegister("xxx", "test:name=Test", "stats", obj2));
        assertSame(obj2, mBeanServerRegistry.getOrRegister("xxx", "test:name=Test", "stats2", obj2));
    }

}
