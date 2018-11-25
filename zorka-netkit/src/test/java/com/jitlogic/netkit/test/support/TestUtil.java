package com.jitlogic.netkit.test.support;


import com.jitlogic.netkit.http.HttpProtocol;

import java.nio.ByteBuffer;

public class TestUtil {

    public static ByteBuffer sbuf(String...lines) {
        return sbuf(true, lines);
    }

    public static ByteBuffer sbuf0(String...lines) {
        return sbuf(false, lines);
    }

    private static ByteBuffer sbuf(boolean crlf, String...lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s);
            if (crlf) sb.append(HttpProtocol.CRLF);
        }
        return ByteBuffer.wrap(sb.toString().getBytes());
    }

}
