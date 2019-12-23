package com.jitlogic.zorka.common.test.cbor;


import com.jitlogic.zorka.common.cbor.Base64FormattingStream;
import com.jitlogic.zorka.common.cbor.ByteArrayCborInput;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import static org.junit.Assert.assertEquals;

public class Base64FormattingUnitTest {

    public String encodeByByte(String input) {
        Base64FormattingStream is = new Base64FormattingStream(new ByteArrayCborInput(input.getBytes()));

        StringBuilder sb = new StringBuilder();

        for (int c = is.read(); c != -1; c = is.read()) {
            sb.append((char)c);
        }

        return sb.toString();
    }

    public void checkByByte(String s) {
        assertEquals("String: '" + s + "'", printBase64Binary(s.getBytes()), encodeByByte(s));
    }

    @Test
    public void testEncodeDecodeByteByByte() {
        checkByByte("X");
        checkByByte("XY");
        checkByByte("XYZ");
    }

    public void checkByBuf(String input, int offs, int len) throws IOException {
        Base64FormattingStream is = new Base64FormattingStream(new ByteArrayCborInput(input.getBytes()));
        for (int i = 0; i < offs; i++) is.read();

        byte[] b = new byte[len];
        is.read(b);

        String s1 = new String(b, "UTF-8");
        String s2 = DatatypeConverter.printBase64Binary(input.getBytes()).substring(offs, offs+len);

        assertEquals("String: '" + input + "', offs=" + offs + ", len=" + len, s2, s1);
    }

    @Test
    public void checkByBuf() throws Exception {
        String S = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

        for (int offs = 0; offs < 6; offs++) {
            for (int len = 1; len < 6; len++) {
                checkByBuf(S, offs, len);
            }
        }
    }
}
