package com.jitlogic.netkit.util;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class BufStreamOutput implements BufHandler {

    private OutputStream os;

    public BufStreamOutput(OutputStream os) {
        this.os = os;
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        try {
            for (ByteBuffer buf : buffers) {
                os.write(buf.array(), buf.position(), buf.limit());
                buf.clear();
            }
        } catch (IOException e) {
            throw new NetException("I/O error when writing bufs to output stream", e);
        }
        return true;
    }
}
