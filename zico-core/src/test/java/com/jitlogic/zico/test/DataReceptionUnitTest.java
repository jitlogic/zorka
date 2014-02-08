/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.test;

import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DataReceptionUnitTest extends ZicoFixture {

    @Test
    public void testAcquireSingleStoreAndCheckIfItWorks() throws Exception {
        HostStore store = hostStoreManager.getHost("test", true);
        SymbolRegistry symbols = store.getSymbolRegistry();
        int t1 = symbols.symbolId("t1"), t2 = symbols.symbolId("t2");

        assertEquals(t2, symbols.symbolId("t2"));
        assertEquals(t1, symbols.symbolId("t1"));
        assertNotEquals(t1, t2);

        //assertNotNull(store.getRdsData());
    }

}
