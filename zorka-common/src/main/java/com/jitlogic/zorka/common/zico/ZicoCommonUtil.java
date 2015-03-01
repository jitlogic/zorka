/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import org.fressian.FressianReader;
import org.fressian.FressianWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ZicoCommonUtil {

    private static ZorkaLog log = ZorkaLogger.getLog(ZicoCommonUtil.class);

    /**
     * Unpacks list of (fressian-encoded) objects from byte array.
     *
     * @param data input data
     * @return list of unpacked objects
     */
    public static List<Object> unpack(byte[] data) {
        List<Object> lst = new ArrayList<Object>();
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            FressianReader r = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            while (is.available() > 0) {
                lst.add(r.readObject());
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Should not happen.", e);
        }
        return lst;
    }


    /**
     * Packs list of fressian-encodable objects and returns encoded byte buffer.
     *
     * @param data objects to be packed
     * @return bytes with encoded data
     */
    public static byte[] pack(Object... data) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
            for (Object d : data) {
                writer.writeObject(d);
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Should not happen.", e);
        }
        return os.toByteArray();
    }


}
