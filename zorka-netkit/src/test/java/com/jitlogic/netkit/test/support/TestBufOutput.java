package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetCtx;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class TestBufOutput implements BufHandler {

    private StringBuilder sb = new StringBuilder();
    private BufHandler output;

    private int numCloses = 0;

    public TestBufOutput() {

    }

    public TestBufOutput(BufHandler output) {
        this.output = output;
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        for (ByteBuffer buf : buffers) {
            if (buf == NetCtx.CLOSE) {
                numCloses++;
            } else {
                int pos = buf.position();
                byte[] b = new byte[buf.remaining()];
                buf.get(b);
                sb.append(new String(b));
                if (output != null) {
                    buf.position(pos);
                }
            }
        }
        return output == null || output.submit(key, copyOnSchedule, buffers);
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public void clear() {
        sb = new StringBuilder();
        numCloses = 0;
    }

    public int getNumCloses() {
        return numCloses;
    }
}
