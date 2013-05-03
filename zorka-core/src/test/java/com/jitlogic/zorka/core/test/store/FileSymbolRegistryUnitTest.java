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

package com.jitlogic.zorka.core.test.store;

import com.jitlogic.zorka.core.store.file.FileSymbolRegistry;
import com.jitlogic.zorka.core.store.SymbolRegistry;

import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;

import java.io.File;
import java.util.Queue;

import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;
import static org.fest.assertions.Assertions.assertThat;

public class FileSymbolRegistryUnitTest extends ZorkaFixture {

    private SymbolRegistry open(String fname) {
        SymbolRegistry reg = new FileSymbolRegistry(zorka.path(getTmpDir(), fname));

        method("load").in(reg).invoke();
        method("open").in(reg).invoke();
        return reg;
    }

    private void close(SymbolRegistry reg) {
        Queue queue = field("submitQueue").ofType(Queue.class).in(reg).get();

        while (queue.size() > 0) {
            method("runCycle").in(reg).invoke();
        }

        method("flush").in(reg).invoke();
        method("close").in(reg).invoke();
    }


    @Test
    public void testCheckEmptySymbolRegistry() throws Exception {
        SymbolRegistry reg = open("symbols.dat");
        assertThat(reg.size()).isEqualTo(0);
    }


    @Test
    public void testAddSomeValuesAndCheckOutputFileSize() throws Exception {
        SymbolRegistry reg = open("symbols.dat");
        reg.symbolId("test1");
        close(reg);

        assertThat(new File(zorka.path(getTmpDir(), "symbols.dat")).length()).isGreaterThan(0);
    }

    @Test
    public void testSaveAndReloadSymbols() throws Exception {
        SymbolRegistry reg1 = open("sym.dat");
        int id1 = reg1.symbolId("test1");
        int id2 = reg1.symbolId("test2");
        close(reg1);

        SymbolRegistry reg2 = open("sym.dat");
        assertThat(reg2.size()).isEqualTo(2);
        assertThat(reg2.symbolName(id1).equals("test1"));
        assertThat(reg2.symbolName(id2).equals("test2"));
        close(reg2);
    }

}
