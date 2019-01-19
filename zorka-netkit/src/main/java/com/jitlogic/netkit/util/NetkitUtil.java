/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.netkit.util;

import com.jitlogic.netkit.ArgumentException;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

public class NetkitUtil {


    public static ByteBuffer toByteBuffer(Object obj) {

        if (obj instanceof ByteBuffer)
            return (ByteBuffer)obj;

        if (obj instanceof byte[])
            return ByteBuffer.wrap((byte[])obj);

        if (obj instanceof String)
            return ByteBuffer.wrap(obj.toString().getBytes());

        if (obj instanceof InputStream) {
            InputStream is = (InputStream) obj;
            return getByteBuffer(is);
        }

        if (obj instanceof File) {
            try {
                InputStream is = new FileInputStream((File) obj);
                return getByteBuffer(is);
            } catch (IOException e) {
                
            }
        }

        // TODO this is insecure and should be controlled
        return ByteBuffer.wrap((""+obj).getBytes());
    }

    private static ByteBuffer getByteBuffer(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[512];
            for (int len = is.read(buf); len >= 0; len = is.read(buf)) {
                os.write(buf, 0, len);
            }
        } catch (IOException e) {
            // TODO handle error
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // TODO handle error
            }
        }
        return ByteBuffer.wrap(os.toByteArray());
    }

    public static byte[] toByteArray(Object obj) {
        if (obj instanceof byte[])
            return (byte[])obj;

        if (obj instanceof String)
            return ((String)obj).getBytes();

        if (obj instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer)obj;
            byte[] rslt = new byte[bb.remaining()];
            System.arraycopy(bb.array(), bb.position(), rslt, 0, bb.remaining());
            return rslt;
        }

        if (obj instanceof InputStream) {
            return toByteArray(getByteBuffer((InputStream)obj));
        }
        if (obj  instanceof File) {
            try {
                return toByteArray(getByteBuffer(new FileInputStream((File)obj)));
            } catch (IOException e) {

            }
        }

        return null;
    }

    public static DynamicBytes readAll(InputStream is) throws IOException {
        DynamicBytes bytes = new DynamicBytes(32768); // init 32k
        byte[] buffer = new byte[16384];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bytes.append(buffer, read);
        }
        is.close();
        return bytes;
    }


    public static ByteBuffer readAll(File f) throws IOException {
        int length = (int) f.length();
        if (length >= 1024 * 1024 * 20) { // 20M
            FileInputStream fs = new FileInputStream(f);
            return fs.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
        } else {
            return ByteBuffer.wrap(readContent(f, length));
        }
    }


    public static byte[] readContent(File f, int length) throws IOException {
        byte[] bytes = new byte[length];
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(f);
            int offset = 0;
            while (offset < length) {
                offset += fs.read(bytes, offset, length - offset);
            }
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (Exception ignore) {
                }
            }
        }
        return bytes;
    }


    public static int coerceInt(Object v) {
        if (v == null) {
            throw new ArgumentException("Expected integer, got null.");
        } else if (v instanceof Number) {
            return ((Number)v).intValue();
        } else if (v.getClass() == String.class) {
            try {
                return Integer.parseInt((String) v);
            } catch (NumberFormatException e) {
                throw new ArgumentException("Expected integer, got '" + v + "'");
            }
        }
        throw new ArgumentException("Expected integer, got " + v.getClass().getName());
    }


    public static String coerceStr(Object v, Pattern regex) {
        if (v == null) {
            throw new ArgumentException("Expected string, got null");
        }
        String s = v.toString();
        if (regex != null && !regex.matcher(s).matches()) {
            throw new ArgumentException("Expected string matching '" + regex + "', got '" + s + "'");
        }
        return s;
    }

    public static boolean coerceBool(Object v) {
        return v != null && !Boolean.FALSE.equals(v);
    }

    public static <T> T coerceObj(Object v, Class<T> clazz, String msg) {
        if (v == null) {
            throw new ArgumentException(msg + ": expected " + clazz.getName() + ", got null");
        }
        if (!clazz.isInstance(v)) {
            throw new ArgumentException(msg + ": expected " + clazz.getName() + ", got " + v.getClass().getName());
        }
        return (T)v;
    }

    public static String keyToString(SelectionKey key) {
        String s = key.toString();
        int ops = key.isValid() ? key.interestOps() : 0;
        return "Key(" + s.substring(s.length()-5) + "," +
                ((ops & SelectionKey.OP_ACCEPT) != 0  ? "A" : "-") +
                ((ops & SelectionKey.OP_CONNECT) != 0  ? "C" : "-") +
                ((ops & SelectionKey.OP_READ) != 0 ? "R" : "-") +
                ((ops & SelectionKey.OP_WRITE) != 0  ? 'W' : '-') + ")";
    }

    public static final Executor RUN = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();;
        }
    };

    public static void close(Closeable obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (IOException e) {
                // Nothing here
            }
        }
    }

    public static void close(Socket obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (IOException e) {
                // Nothing here
            }
        }
    }

}
