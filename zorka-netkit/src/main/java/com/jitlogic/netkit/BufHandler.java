package com.jitlogic.netkit;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface BufHandler {

    boolean submit(final SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers);
}
