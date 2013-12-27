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
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.PersistentSymbolRegistry;
import com.jitlogic.zico.test.support.ZicoFixture;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

import java.io.File;

public class PersistentSymbolRegistryUnitTest extends ZicoFixture {

    private PersistentSymbolRegistry syms;

    @After
    public void tearDown() throws Exception {
        if (syms != null) {
            syms.close();
            syms = null;
        }
    }

    @Test
    public void testCreateAndRereadSymbolRegistry() throws Exception {
        File path = new File(getTmpDir(), "symbols.dat");
        syms = new PersistentSymbolRegistry(path);
        syms.put(10, "someSymbol");
        int id1 = syms.symbolId("anotherSymbol");

        assertThat(id1)
            .describedAs("Each new symbol should have greated ID than all previos symbols (including symbols added manually")
            .isGreaterThan(10);

        syms.close();

        syms = new PersistentSymbolRegistry(path);
        assertEquals(10, syms.symbolId("someSymbol"));
        assertEquals("anotherSymbol", syms.symbolName(id1));
        syms.close();
    }

    @Test
    public void testCreateAndAppendToSymbolRegistry() throws Exception {
        File path = new File(getTmpDir(), "symbols.dat");
        syms = new PersistentSymbolRegistry(path);
        syms.put(10, "someSymbol");
        syms.close();

        syms = new PersistentSymbolRegistry(path);
        syms.put(11, "otherSymbol");
        syms.close();

        syms = new PersistentSymbolRegistry(path);
        assertEquals("someSymbol", syms.symbolName(10));
        assertEquals("otherSymbol", syms.symbolName(11));
        syms.close();
    }

    @Test
    public void testExportImportSymbolRegistry() throws Exception {
        File path = new File(getTmpDir(), "symbols.dat");
        syms = new PersistentSymbolRegistry(path);
        int someId = syms.symbolId("someSymbol");
        int otherId = syms.symbolId("otherSymbol");
        syms.export();
        syms.close();

        path.delete();

        syms = new PersistentSymbolRegistry(path);
        assertEquals("someSymbol", syms.symbolName(someId));
        assertEquals("otherSymbol", syms.symbolName(otherId));
        syms.close();
    }
    // Test export and import capabilities

}
