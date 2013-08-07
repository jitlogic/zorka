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
package com.jitlogic.zorka.central.test;

import com.jitlogic.zorka.central.Store;
import com.jitlogic.zorka.central.SymbolSet;
import com.jitlogic.zorka.central.test.support.CentralFixture;

import com.jitlogic.zorka.common.util.ObjectInspector;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataReceptionUnitTest extends CentralFixture {

    @Test
    public void testAcquireSingleStoreAndCheckIfItWorks() throws Exception {
        Store store = storeManager.get("test");
        SymbolSet symbols = store.getSymbols();
        int t1 = symbols.get("t1"), t2 = symbols.get("t2");

        assertEquals(t2, symbols.get("t2"));
        assertEquals(t1, symbols.get("t1"));
        assertNotEquals(t1, t2);

        assertNotNull(store.getRds());
        assertNotNull(store.getTraces());
    }


    @Test
    public void testIfStorageManagerProperlyClosesAllStores() throws Exception {
        Store store = storeManager.get("test");
        store.getSymbols();

        assertNotNull(ObjectInspector.getField(store, "symbols"));
        storeManager.close();
        assertNull(ObjectInspector.getField(store, "symbols"));
    }
}
