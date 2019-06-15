package com.jitlogic.zorka.common.http;

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implemented from scratch because Java's BufferedReader is FUBAR'd when it comes to CR/LF/CRLF
 * handling. Shittiness of various parts of Java APIs is sometimes screamingly obvious.
 */
public class BufferedStreamLineReader {

    private int maxLine;
    private byte[] buf;
    private InputStream is;

    public BufferedStreamLineReader(InputStream is, int maxLine) {
        this.is = is;
        this.maxLine = maxLine;
        buf = new byte[128];
    }

    public String readLine() {
        try {
            int pos = 0;
            for (int ch = is.read(); ch >= 0; ch = is.read()) {
                if (ch == '\n') {
                    return new String(ZorkaUtil.clipArray(buf, pos));
                } else if (ch != '\r') {
                    if (pos >= buf.length) buf = ZorkaUtil.clipArray(buf, Math.min(buf.length + 128, maxLine));
                    buf[pos] = (byte)ch;
                    pos++;
                }
            }
            return new String(ZorkaUtil.clipArray(buf, pos));
        } catch (IOException e) {
            throw new HttpException("I/O error reading", 503, "", null, e);
        }
    }

    public byte[] readData(int len) {
        int pos = 0;
        byte[] buf = new byte[len];

        try {
            for (int r = is.read(buf); r >= 0 && pos < len; r = is.read(buf, pos, len - pos)) {
                pos += r;
            }
        } catch (IOException e) {
            throw new HttpException("I/O error reading", 503, "", null, e);
        }

        return buf;
    }

}
