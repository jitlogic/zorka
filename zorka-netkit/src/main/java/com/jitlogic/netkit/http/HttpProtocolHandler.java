package com.jitlogic.netkit.http;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetCtx;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static com.jitlogic.netkit.http.HttpProtocol.H_CONNECTION;

public class HttpProtocolHandler implements BufHandler {

    private HttpConfig config;
    private HttpDecoder decoder;

    public HttpProtocolHandler(HttpConfig config, HttpListener listener) {
        this(config, HttpDecoderState.READ_REQ_LINE, listener);
    }

    public HttpProtocolHandler(HttpConfig config, HttpDecoderState initialState, HttpListener listener) {
        this.decoder = new HttpDecoder(config, initialState, listener);
        this.config = config;
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        return decoder.submit(key, copyOnSchedule, buffers);
    }

    public static void errorResponse(HttpConfig config, SelectionKey key, int status, String message) {
        HttpEncoder enc = new HttpEncoder(config);
        enc.response(key, HttpProtocol.HTTP_1_1, status, "");
        enc.header(key, H_CONNECTION, "close");
        enc.body(key, ByteBuffer.wrap(message.getBytes()), NetCtx.CLOSE);
        enc.finish(key);
    }
}
