package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.NetCtx;
import com.jitlogic.netkit.http.*;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class TestMessageListener implements HttpMessageListener {

    private HttpConfig config = new HttpConfig();
    private List<HttpMessage> reqs = new ArrayList<HttpMessage>();
    private LinkedList<HttpMessage> replies = new LinkedList<HttpMessage>();
    private boolean verbose = false;

    public TestMessageListener(HttpMessage...replies) {
        this("TEST", false, replies);
    }

    public TestMessageListener(String tag, boolean verbose, HttpMessage...replies) {
        addReplies(replies);
    }

    public void addReplies(HttpMessage...replies) {
        this.replies.addAll(Arrays.asList(replies));
    }

    @Override
    public void submit(SelectionKey key, HttpMessage message) {
        reqs.add(message);
        NetCtx ctx = NetCtx.fromKey(key);
        assertNotNull("NetContext not passed", ctx);
        assertNotNull("Output not passed", ctx.getOutput());
        if (!replies.isEmpty()) {
            HttpMessage msg = replies.pollFirst();
            HttpMessageHandler mh = new HttpMessageHandler(config,null);
            mh.submit(key, msg);
        }
    }

    public List<HttpMessage> getReqs() {
        return reqs;
    }
}
