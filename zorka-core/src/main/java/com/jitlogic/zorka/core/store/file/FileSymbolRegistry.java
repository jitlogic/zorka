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

package com.jitlogic.zorka.core.store.file;

import com.jitlogic.zorka.core.util.ByteBuffer;
import com.jitlogic.zorka.core.store.SymbolRegistry;
import com.jitlogic.zorka.core.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.util.ZorkaLogger;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FileSymbolRegistry extends ZorkaAsyncThread<byte[]> implements SymbolRegistry {

    /** Output file path */
    private String path;

    /** Output file */
    private OutputStream output;

    /** ID of last symbol added to registry. */
    private AtomicInteger lastSymbolId = new AtomicInteger(0);

    /** Symbol name to ID map */
    private ConcurrentHashMap<String,Integer> symbolIds = new ConcurrentHashMap<String, Integer>();

    /** Symbol ID to name map */
    private ConcurrentHashMap<Integer,String> symbolNames = new ConcurrentHashMap<Integer,String>();


    public FileSymbolRegistry(String path) {
        super("file-symbol-registry");
        this.path = path;
    }


    @Override
    public int symbolId(String symbol) {
        if (symbol != null) {
            Integer id = symbolIds.get(symbol);

            if (id == null) {
                int newId = lastSymbolId.incrementAndGet();

                log.debug(ZorkaLogger.ZTR_SYMBOL_REGISTRY, "Adding symbol '%s', newid=%s", symbol, newId);

                id = symbolIds.putIfAbsent(symbol, newId);
                if (id == null) {
                    symbolNames.put(newId, symbol);
                    submit(serialize(newId, symbol));
                    id = newId;
                }
            }

            return id;
        } else {
            return 0;
        }
    }


    private byte[] serialize(int id, String name) {
        ByteBuffer buf = new ByteBuffer(name.length() + 6);
        buf.putInt(id);
        buf.putString(name);
        return buf.getContent();
    }



    @Override
    public String symbolName(int symbolId) {
        if (symbolId == 0) {
            return "<null>";
        }
        return symbolNames.get(symbolId);
    }


    @Override
    public void put(int symbolId, String symbol) {

        log.debug(ZorkaLogger.ZTR_SYMBOL_REGISTRY, "Putting symbol '%s', newid=%s", symbol, symbolId);

        symbolIds.put(symbol, symbolId);
        symbolNames.put(symbolId, symbol);

        submit(serialize(symbolId, symbol));

        // TODO not thread safe !
        if (symbolId > lastSymbolId.get()) {
            lastSymbolId.set(symbolId);
        }
    }


    @Override
    public int size() {
        return symbolNames.size();
    }


    @Override
    protected void process(byte[] chunk) {
        try {
            output.write(chunk);
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Cannot write to " + path + ". Reopening file.", e);
            reopen();
            submit(chunk);
        } catch (NullPointerException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Attempt to write to closed file: " + path);
            reopen();
            submit(chunk);
        }
    }


    private void reopen() {
        log.info(ZorkaLogger.ZCL_CONFIG, "Reopening symbols file: " + path);
        close();
        open();
    }


    public void load() {
        File f = new File(path);
        if (f.isFile() && f.canRead()) {
            log.info(ZorkaLogger.ZCL_CONFIG, "Loading symbols from " + path);
            int len = (int)f.length();
            byte[] b = new byte[len];
            InputStream is = null;

            try {
                is = new FileInputStream(f);
                is.read(b);
            } catch (FileNotFoundException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot read symbols file: " + path, e);
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot read symbols file: " + path, e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        log.error(ZorkaLogger.ZCL_ERRORS, "Cannot close symbols file", e);
                    }
                }
            }

            ByteBuffer buf = new ByteBuffer(b);

            while (!buf.eof()) {
                int id = buf.getInt();
                String sym = buf.getString();
                symbolIds.put(sym, id);
                symbolNames.put(id, sym);
            }
        }
    }


    @Override
    protected synchronized void open() {
        if (output == null) {
            try {
                output = new FileOutputStream(path, true);
            } catch (FileNotFoundException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot open symbols file for write: " + path, e);
            }
        }
    }


    @Override
    protected synchronized void close() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot close symbols file: " + path, e);
            }
            output = null;
        }
    }


    @Override
    protected void flush() {
        if (output != null) {
            try {
                output.flush();
            } catch (IOException e) {
                log.warn(ZorkaLogger.ZCL_ERRORS, "Cannot flush symbols file: " + path, e);
            }
        }
    }

}
