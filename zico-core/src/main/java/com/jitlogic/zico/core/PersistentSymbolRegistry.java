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
package com.jitlogic.zico.core;

import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.fressian.FressianReader;
import org.fressian.FressianWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class PersistentSymbolRegistry extends SymbolRegistry implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(PersistentSymbolRegistry.class);

    private File path;
    private OutputStream os;
    private FressianWriter writer;

    public PersistentSymbolRegistry(File path) throws IOException {
        super();

        this.path = path;

        open();
    }

    public void open() throws IOException {

        int maxid = 0;

        if (path.exists()) {
            try (InputStream is = new FileInputStream(path)) {
                FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
                Object obj;
                for (obj = reader.readObject(); obj != null; obj = reader.readObject()) {
                    if (obj instanceof Symbol) {
                        Symbol sym = (Symbol)obj;
                        symbolNames.put(sym.getId(), sym.getName());
                        symbolIds.put(sym.getName(), sym.getId());
                        maxid = Math.max(maxid, sym.getId());
                    }
                }
            } catch (IOException e) {

            }
        }

        lastSymbolId.set(maxid);

        os = new FileOutputStream(path, true);
        writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
    }


    @Override
    protected void persist(int id, String name) {
        try {
            writer.writeObject(new Symbol(id, name));
            os.flush();
        } catch (IOException e) {
            log.error("Cannot persist symbol '" + name + "' in " + path, e);
        }
    }


    @Override
    public void close() throws IOException {
        writer.close();
        writer = null; os = null;
    }
}
