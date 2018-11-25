package com.jitlogic.netkit.http;

import java.nio.channels.SelectionKey;

public interface HttpMessageListener {

    void submit(SelectionKey key, HttpMessage message);

}
