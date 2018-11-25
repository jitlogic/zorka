package com.jitlogic.netkit;

import java.nio.channels.SocketChannel;

public interface NetCtxFactory {

    NetCtxFactory DEFAULT = new NetCtxFactory() {
        @Override
        public NetCtx create(SocketChannel ch, BufHandler in, BufHandler out) {
            return new NetCtx(ch, in, out);
        }
    };

    NetCtx create(SocketChannel ch, BufHandler in, BufHandler out);
}
