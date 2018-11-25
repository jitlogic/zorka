package com.jitlogic.netkit.test.support;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class TestSelectionKey extends SelectionKey {

    private String name;

    public TestSelectionKey(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Key("+name+")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TestSelectionKey &&
                ((TestSelectionKey)o).name.equals(name);
    }

    @Override
    public SelectableChannel channel() {
        return null;
    }

    @Override
    public Selector selector() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public int interestOps() {
        return 0;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        return null;
    }

    @Override
    public int readyOps() {
        return 0;
    }
}
