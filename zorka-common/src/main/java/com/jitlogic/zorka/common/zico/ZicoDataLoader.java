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
package com.jitlogic.zorka.common.zico;

import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.fressian.FressianReader;
import org.fressian.FressianWriter;

import java.io.*;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


public class ZicoDataLoader {


    private ZicoClientConnector conn;


    public ZicoDataLoader(String addr, int port, String hostname, String auth) throws IOException {
        conn = new ZicoClientConnector(addr, port);
        conn.connect();
        conn.hello(hostname, auth);
    }


    public static InputStream open(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] hdr = new byte[4]; fis.read(hdr);

        if (hdr[0] != 'Z' || hdr[1] != 'T' || hdr[2] != 'R') {
            throw new IOException("Invalid header (invalid file type).");
        }

        if (hdr[3] == 'Z') {
            InputStream is = new BufferedInputStream(new InflaterInputStream(fis, new Inflater(true), 65536));
            return is;
        } else if (hdr[3] == 'C') {
            return new BufferedInputStream(fis);
        } else {
            throw new IOException("Invalid header (invalid file type).");
        }
    }


    public void load(String path) {
        InputStream is = null;
        try {
            is = open(new File(path));
            load(is);
        } catch (IOException e) {
            e.printStackTrace();
            try { is.close(); } catch (IOException e1) {

            }
        }
    }

    public void loadXX(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int b = is.read(); b != -1; b = is.read()) {
            os.write(b);
        }
        conn.send(ZicoPacket.ZICO_DATA, os.toByteArray());
        ZicoPacket rslt = conn.recv();
        if (rslt.getStatus() != ZicoPacket.ZICO_OK) {
            throw new ZicoException(rslt.getStatus(), "Error submitting data.");
        }
    }

    public void load(InputStream is) throws IOException {
        FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);

        Object obj;

        while (null != (obj = reader.readObject())) {
            writer.writeObject(obj);
            if (obj instanceof TraceRecord && os.size() > 1024*1024) {
                conn.send(ZicoPacket.ZICO_DATA, os.toByteArray());
                ZicoPacket rslt = conn.recv();
                if (rslt.getStatus() != ZicoPacket.ZICO_OK) {
                    throw new ZicoException(rslt.getStatus(), "Error submitting data.");
                }
                os = new ByteArrayOutputStream();
                writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
                System.out.print(".");
            }
        }
    }

}
