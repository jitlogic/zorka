package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetRequest;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;

public class EchoRequest extends NetRequest {

    private CountDownLatch latch = new CountDownLatch(1);
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private String reply;

    public EchoRequest(String addr, int port) {
        super(addr, port);
    }

    @Override
    public void sendRequest(SelectionKey key, BufHandler output) {
        output.submit(key, false, ByteBuffer.wrap("HELO\n".getBytes()));
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        for (ByteBuffer buf : buffers) {
            while (buf.hasRemaining()) {
                bos.write(buf.get());
            }
        }
        reply = new String(bos.toByteArray());
        latch.countDown();
        return true;
    }

    public String getReply() throws InterruptedException {
        latch.await();
        return reply;
    }

}
