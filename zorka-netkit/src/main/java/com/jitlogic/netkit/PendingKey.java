package com.jitlogic.netkit;

import java.nio.channels.SelectionKey;

class PendingKey {
    public final SelectionKey key;
    // operation: can be register for write or close the selectionkey
    public final int Op;

    PendingKey(SelectionKey key, int op) {
        this.key = key;
        Op = op;
    }

    public static final int OP_WRITE = -1;
    public static final int OP_HANDSHAKE = -2;
}
