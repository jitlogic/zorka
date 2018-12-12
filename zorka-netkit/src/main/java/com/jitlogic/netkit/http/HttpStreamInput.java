package com.jitlogic.netkit.http;

import com.jitlogic.netkit.NetException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class HttpStreamInput extends HttpMessageHandler implements HttpListener, Runnable {

    private HttpDecoder decoder;
    private InputStream is;

    public HttpStreamInput(HttpConfig config, HttpMessageListener listener,
                           HttpDecoderState initialState, InputStream is) {
        super(config, listener);
        this.decoder = new HttpDecoder(config, initialState, this);
        this.is = is;
    }

    @Override
    public void run() {
        byte[] buf = new byte[1024];

        try {
            for (int n = is.read(buf); n >= 0; n = is.read(buf)) {
                decoder.submit(null, false, ByteBuffer.wrap(buf, 0, n));
                if (decoder.hasFinished()) {
                    decoder.reset();
                    break;
                }
            }
        } catch (IOException e) {
            throw new NetException("I/O error", e);
        }

    }
}
