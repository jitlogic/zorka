package com.jitlogic.netkit.http;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.regex.Matcher;

import com.jitlogic.netkit.*;
import com.jitlogic.netkit.log.Logger;
import com.jitlogic.netkit.log.LoggerFactory;
import com.jitlogic.netkit.util.TextUtil;

import static com.jitlogic.netkit.http.HttpProtocol.*;
import static com.jitlogic.netkit.util.TextUtil.CR;
import static com.jitlogic.netkit.util.TextUtil.LF;

public class HttpDecoder implements BufDecoder {

    private Logger log = LoggerFactory.getLogger(HttpDecoder.class);

    public final static String MALFORMED_MSG = "malformed HTTP message";
    public final static String TOO_LARGE_MSG = "too large";

    public static final int BODY_MAX = 8 * 1024 * 1024;
    public static final int HEADERS_MAX = 64 * 1024;
    public static final int LINE_MAX = 32 * 1024;

    private HttpDecoderState state;
    private final HttpDecoderState initialState;
    private int readRemaining = 0; // bytes need read
    private int readCount = 0; // already read bytes count

    private byte[] content;

    private final int maxBody;
    private final int maxLine;

    private byte[] lineBuffer = new byte[128];
    private int lineBufferIdx = 0;
    private boolean readCR = false;

    private HttpConfig config;
    private final HttpListener listener;

    public HttpDecoder(HttpConfig config, HttpDecoderState initialState, HttpListener listener) {
        this.config = config;
        this.maxLine = config.getMaxLineSize();
        this.maxBody = config.getMaxBodySize();
        this.initialState = initialState;
        this.listener = listener;
        this.state = initialState;
    }

    private void error(SelectionKey key, String message, Object data, Throwable e) {
        // TODO properly handle errors
        listener.error(key, 400, message, data, e);
        state = HttpDecoderState.ERROR;
    }

    public String readLine(SelectionKey key, ByteBuffer buffer) {
        byte b;
        boolean more = true;
        while (buffer.hasRemaining() && more) {
            b = buffer.get();

            if (readCR && b != LF) {
                error(key, MALFORMED_MSG, "malformed CRLF", null);
                return null;
            }

            if (b == CR) {
                readCR = true;
            } else if (b == LF) {
                more = false;
            } else {
                if (lineBufferIdx == maxLine - 2) {
                    error(key, TOO_LARGE_MSG, "", null);
                    return null;
                }
                if (lineBufferIdx == lineBuffer.length) {
                    lineBuffer = Arrays.copyOf(lineBuffer, lineBuffer.length * 2);
                }
                lineBuffer[lineBufferIdx] = b;
                ++lineBufferIdx;
            }
        }
        String line = null;
        if (!more) {
            line = new String(lineBuffer, 0, lineBufferIdx);
            this.lineBufferIdx = 0;
            this.readCR = false;
        }
        return line;
    }


    private void parseResponse(SelectionKey key, String line) {
        Matcher m = RE_RESP_LINE.matcher(line);
        if (m.matches()) {
            listener.response(key, m.group(1), Integer.parseInt(m.group(2)), m.group(3));
            state = HttpDecoderState.READ_HEADER;
        } else {
            error(key, MALFORMED_MSG, line, null);
        }

    }


    private void parseRequest(SelectionKey key, String line) {
        Matcher m = RE_REQ_LINE.matcher(line);
        if (m.matches()) {
            listener.request(key, m.group(4), HttpMethod.valueOf(m.group(1)), m.group(2), m.group(3));
            state = HttpDecoderState.READ_HEADER;
        } else {
            error(key, MALFORMED_MSG, line, null);
        }
    }

    private void decode(SelectionKey key, ByteBuffer buffer) {
        String line;
        while (buffer.hasRemaining() && state != HttpDecoderState.ERROR) {
            switch (state) {
                case ALL_READ:
                    reset();
                    break;
                case READ_REQ_LINE:
                    line = readLine(key, buffer);
                    if (line != null && line.length() > 0)
                        parseRequest(key, line);
                    break;
                case READ_RESP_LINE:
                    line = readLine(key, buffer);
                    if (line != null && line.length() > 0)
                        parseResponse(key, line);
                    break;
                case READ_HEADER:
                    parseHeaders(key, buffer);
                    break;
                case READ_CHUNK_SIZE:
                    line = readLine(key, buffer);
                    if (line != null) {
                        readRemaining = getChunkSize(key, line);
                        if (readRemaining == 0) {
                            state = HttpDecoderState.READ_CHUNK_FOOTER;
                        } else if (readRemaining > 0) {
                            if (readCount + readRemaining > maxBody) {
                                error(key, TOO_LARGE_MSG, line, null);
                                return;
                            }
                            if (content == null) {
                                content = new byte[readRemaining];
                            } else if (content.length < readCount + readRemaining) {
                                // *1.3 to protect slow client
                                int newLength = (int) ((readRemaining + readCount) * 1.3);
                                content = Arrays.copyOf(content, newLength);
                            }
                            state = HttpDecoderState.READ_CHUNKED_CONTENT;
                        }
                    }
                    break;
                case READ_FIXED_LENGTH_CONTENT:
                    readFixedLength(buffer);
                    if (readRemaining == 0) {
                        finish(key);
                    }
                    break;
                case READ_CHUNKED_CONTENT:
                    readFixedLength(buffer);
                    if (readRemaining == 0) {
                        state = HttpDecoderState.READ_CHUNK_DELIMITER;
                    }
                    break;
                case READ_CHUNK_FOOTER:
                    readEmptyLine(buffer);
                    finish(key);
                    break;
                case READ_CHUNK_DELIMITER:
                    readEmptyLine(buffer);
                    state = HttpDecoderState.READ_CHUNK_SIZE;
                    break;
            }
        }
    }

    private void finish(SelectionKey key) {
        state = HttpDecoderState.ALL_READ;
        listener.body(key, ByteBuffer.wrap(content, 0, readCount));
        listener.finish(key);
    }

    private void readEmptyLine(ByteBuffer buffer) {
        byte b = buffer.get();
        if (b == TextUtil.CR && buffer.hasRemaining()) {
            buffer.get(); // should be LF
        }
    }

    private void readFixedLength(ByteBuffer buffer) {
        int toRead = Math.min(buffer.remaining(), readRemaining);
        buffer.get(content, readCount, toRead);
        readRemaining -= toRead;
        readCount += toRead;
    }


    private void parseHeaders(SelectionKey key, ByteBuffer buffer) {
        String line = readLine(key, buffer);

        boolean chunked = false;
        String cl = null;

        while (line != null && !line.isEmpty()) {
            Matcher m = RE_HEADER.matcher(line);
            if (m.matches()) {
                String k = m.group(1), v = m.group(2);
                processHeader(key, k, v);
                chunked |= H_TRANSFER_ENCODING.equalsIgnoreCase(k) && "chunked".equalsIgnoreCase(v);
                if (H_CONTENT_LENGTH.equalsIgnoreCase(k)) {
                    cl = v;
                }
            } else {
                state = HttpDecoderState.ERROR;
                listener.error(key, 400, MALFORMED_MSG, line, null);
                return;
            }
            line = readLine(key, buffer);
        }

        if (line == null) {
            return;
        }

        if (chunked) {
            state = HttpDecoderState.READ_CHUNK_SIZE;
        } else {
            processContentLength(key, cl);
        }
    }

    private void processContentLength(SelectionKey key, String cl) {
        if (cl != null) {
            try {
                readRemaining = Integer.parseInt(cl);
                if (readRemaining > 0) {
                    if (readCount + readRemaining > maxBody) {
                        error(key, TOO_LARGE_MSG, cl, null);
                        return;
                    }
                    content = new byte[readRemaining];
                    state = HttpDecoderState.READ_FIXED_LENGTH_CONTENT;
                } else {
                    state = HttpDecoderState.ALL_READ;
                    listener.finish(key);
                }
            } catch (NumberFormatException e) {
                error(key, MALFORMED_MSG, "Content-Length: " + cl, e);
            }
        } else {
            state = HttpDecoderState.ALL_READ;
            listener.finish(key);
        }
    }

    private void processHeader(SelectionKey key, String k, String v) {
        if (v.indexOf(',') != -1) {
            for (String s : v.split(",")) {
                listener.header(key, k, s);
            }
        } else {
            listener.header(key, k, v);
        }
    }

    @Override
    public void reset() {
        state = initialState;
        readCount = 0;
        content = null;
        this.lineBufferIdx = 0;
        this.readCR = false;
    }

    @Override
    public boolean hasError() {
        return state == HttpDecoderState.ERROR;
    }

    @Override
    public boolean hasInitial() {
        return state == HttpDecoderState.READ_REQ_LINE || state == HttpDecoderState.READ_RESP_LINE;
    }

    @Override
    public boolean hasFinished() {
        return state == HttpDecoderState.ALL_READ;
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        for (ByteBuffer buffer : buffers) {
            decode(key, buffer);
        }
        return true;
    }

    private int getChunkSize(SelectionKey key, String hex) {
        // TODO switch to regex
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            error(key, MALFORMED_MSG, "Chunk size: " + hex, e);
            return -1;
        }
    }


}
