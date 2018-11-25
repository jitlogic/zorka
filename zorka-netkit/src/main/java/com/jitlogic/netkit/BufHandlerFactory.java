package com.jitlogic.netkit;

import java.nio.channels.SocketChannel;

public interface BufHandlerFactory {
    BufHandler create(SocketChannel ch);
}
