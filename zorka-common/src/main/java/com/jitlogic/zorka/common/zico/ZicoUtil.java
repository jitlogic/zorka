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

import java.io.*;
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


    public static byte[] pack(Object... data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        for (Object d : data) {
            writer.writeObject(d);
        }
        return os.toByteArray();
    }


}
