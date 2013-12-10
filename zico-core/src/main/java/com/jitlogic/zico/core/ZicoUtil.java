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


import com.jitlogic.zico.core.model.SymbolicExceptionInfo;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.SymbolicStackElement;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.objectweb.asm.Type;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ZicoUtil {

    /**
     * Print short class name
     */
    public static final int PS_SHORT_CLASS = 0x01;

    /**
     * Print result type
     */
    public static final int PS_RESULT_TYPE = 0x02;

    /**
     * Print short argument types
     */
    public static final int PS_SHORT_ARGS = 0x04;

    /**
     * Omits arguments overall in pretty pring
     */
    public static final int PS_NO_ARGS = 0x08;


    private static final Pattern RE_DOT = Pattern.compile("\\.");

    public static String shortClassName(String className) {
        String[] segs = RE_DOT.split(className != null ? className : "");
        return segs[segs.length - 1];
    }


    /**
     * Returns human readable method description (with default flags)
     *
     * @return method description string
     */
    public static String prettyPrint(TraceRecord tr, SymbolRegistry sr) {
        return prettyPrint(tr, sr, PS_RESULT_TYPE | PS_SHORT_ARGS);
    }


    /**
     * Returns human readable method description (configurable with supplied flags)
     *
     * @param style style flags (see PS_* constants)
     * @return method description string
     */
    public static String prettyPrint(TraceRecord tr, SymbolRegistry sr, int style) {
        StringBuffer sb = new StringBuffer(128);

        String signature = sr.symbolName(tr.getSignatureId());

        // Print return type
        if (0 != (style & PS_RESULT_TYPE)) {
            Type retType = Type.getReturnType(signature);
            if (0 != (style & PS_SHORT_ARGS)) {
                sb.append(shortClassName(retType.getClassName()));
            } else {
                sb.append(retType.getClassName());
            }
            sb.append(" ");
        }

        // Print class name
        if (0 != (style & PS_SHORT_CLASS)) {
            sb.append(shortClassName(sr.symbolName(tr.getClassId())));
        } else {
            sb.append(sr.symbolName(tr.getClassId()));
        }

        sb.append(".");
        sb.append(sr.symbolName(tr.getMethodId()));
        sb.append("(");

        // Print arguments (if needed)
        if (0 == (style & PS_NO_ARGS)) {
            Type[] types = Type.getArgumentTypes(signature);
            for (int i = 0; i < types.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (0 != (style & PS_SHORT_ARGS)) {
                    sb.append(shortClassName(types[i].getClassName()));
                } else {
                    sb.append(types[i].getClassName());
                }
            }
        }

        sb.append(")");

        return sb.toString();
    }


    public static String prettyPrintSafe(TraceRecord tr, SymbolRegistry sr, int style) {
        try {
            return prettyPrint(tr, sr, style);
        } catch (Exception e) {
            return "<Error: " + e.getMessage() + ">";
        }
    }


    public static long toUIntBE(byte[] b) {
        return (b[0] & 0xffL)
                | ((b[1] & 0xffL) << 8)
                | ((b[2] & 0xffL) << 16)
                | ((b[3] & 0xffL) << 24);
    }


    public static byte[] fromUIntBE(long l) {
        return new byte[]{
                (byte) l,
                (byte) (l >>> 8),
                (byte) (l >>> 16),
                (byte) (l >>> 24)};
    }


    public static long readUInt(RandomAccessFile input) throws IOException {
        byte[] b = new byte[4];

        if (input.read(b) != 4) {
            throw new EOFException("EOF encountered when reading UINT");
        }

        return ZicoUtil.toUIntBE(b);
    }


    public static SymbolicExceptionInfo extractSymbolicExceptionInfo(SymbolRegistry symbolRegistry, SymbolicException sex) {
        SymbolicExceptionInfo sei = new SymbolicExceptionInfo();
        sei.setExClass(symbolRegistry.symbolName(sex.getClassId()));
        sei.setMessage(sex.getMessage());
        List<String> stack = new ArrayList<String>(sex.getStackTrace().length);
        for (SymbolicStackElement sel : sex.getStackTrace()) {
            stack.add("  at " + symbolRegistry.symbolName(sel.getClassId())
                    + "." + symbolRegistry.symbolName(sel.getMethodId())
                    + " [" + symbolRegistry.symbolName(sel.getFileId())
                    + ":" + sel.getLineNum() + "]");
        }
        sei.setStackTrace(stack);
        return sei;
    }

    public static String jsonPackException(SymbolicExceptionInfo ex) throws JSONException {
        return "";
    }

    public static SymbolicExceptionInfo jsonUnpackException(String s) throws JSONException {
        return null;
    }

    public static String jsonPack(Map<String,String> map) throws JSONException {
        JSONObject obj = new JSONObject();
            for (Map.Entry<String,String> e : map.entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }

        return obj.toString();
    }


    // TODO this particular JSON implementation is total cock-up; find and use something better instead;

    public static Map<String,String> jsonUnpack(String s) throws JSONException {
        Map<String,String> map = new HashMap<String, String>();
            if (s != null && s.trim().length() > 0) {
                JSONObject obj = new JSONObject().getJSONObject(s);
                JSONArray names = obj.names();
                for (int i = 0; i < names.length(); i++) {
                    String name = names.getString(i);
                    map.put(name, obj.getString(name));
                }
            }
        return map;
    }

}
