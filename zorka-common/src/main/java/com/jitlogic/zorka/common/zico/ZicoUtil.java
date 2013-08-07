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
import org.fressian.FressianReader;
import org.fressian.FressianWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ZicoUtil {
    public static List<Object> unpack(byte[] data) throws IOException {
        List<Object> lst = new ArrayList<Object>();
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        FressianReader r = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
        while (is.available() > 0) {
            lst.add(r.readObject());
        }
        return lst;
    }

    public static byte[] pack(Object...data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        for (Object d : data) {
            writer.writeObject(d);
        }
        return os.toByteArray();
    }

    /**
     * Seeks for proper protocol signature sequence. Returns just after reading last
     * byte of sought signature.
     *
     * @param is
     *
     * @param signature
     *
     * @throws IOException
     */
    public static void seekSignature(InputStream is, int...signature) throws IOException {
        int[] data = new int[signature.length];
        int n = 0;

        for (int i = 0; i < signature.length; i++) {
            data[i] = is.read();
        }

        do {
            for (n = 0; n < signature.length; n++) {
                if (data[n] != signature[n]) {
                    for (int i = 1; i < data.length; i++) {
                        data[i-1] = data[i];
                    }
                    data[data.length-1] = is.read();
                    break;
                }
            }
        } while (n < signature.length);
    }

}
