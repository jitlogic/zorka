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
package com.jitlogic.zorka.agent;


import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.store.SimplePerfDataFormat;
import com.jitlogic.zorka.core.store.SymbolRegistry;
import com.jitlogic.zorka.core.store.TraceReader;
import com.jitlogic.zorka.core.store.ZorkaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AgentUtil {

    private static final long MB = 1024 * 1024 * 1024;

    private ZorkaStore store;
    private SymbolRegistry symbols;

    public AgentUtil(String root) throws IOException {
        symbols = new SymbolRegistry(new File(root, "symbols.db"));
        store = new ZorkaStore(new File(root, "traces").getPath(), 200*MB, 200*MB, symbols);
        store.open();
    }

    public void storeImport(File file) throws IOException {

        if (!(file.canRead() && file.isFile())) {
            throw new IOException("Path " + file.getPath() + " is not a file");
        }

        byte[] buf = new byte[(int)file.length()];
        InputStream is = new FileInputStream(file);
        is.read(buf); is.close();
        TraceReader reader = new TraceReader(symbols);
        new SimplePerfDataFormat(buf).decode(reader);
        for (TraceRecord rec : reader.getResults()) {
            store.add(rec);
        }
        System.out.println("Traces imported: " + reader.getResults().size());
        symbols.flush();
        store.flush();
    }

}
