package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetCtx;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class EchoHandler implements BufHandler {

    private boolean close;
    private ByteArrayOutputStream os = new ByteArrayOutputStream();

    public EchoHandler(boolean close) {
        this.close = close;
    }

    public synchronized void setClose(boolean close) {
        this.close = close;
    }

    public synchronized boolean isClose() {
        return close;
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        // TODO make this thing fully streaming, do not use ByteArrayOutputStream, make it thread safe
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer buf = buffers[i];
            while (buf.hasRemaining()) {
                byte b = buf.get();
                os.write(b);
                if (b == '\n') {
                    byte[] a = os.toByteArray(); os.reset();
                    ByteBuffer bb = ByteBuffer.wrap(a);
                    BufHandler out = NetCtx.fromKey(key).getOutput();
                    //out.submit(key, true, bb, a[0] == 'q' ? NetAtta.CLOSE : NetAtta.NULL);
                    if (isClose()) {
                        out.submit(key, true, bb, NetCtx.CLOSE);
                    } else {
                        out.submit(key, true, bb);
                    }
                    return !(buf.hasRemaining() || i < buffers.length-1);
                }
            }
        }

        return true;
    }

}
