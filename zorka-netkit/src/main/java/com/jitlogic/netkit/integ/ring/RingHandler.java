package com.jitlogic.netkit.integ.ring;

import clojure.lang.*;
import com.jitlogic.netkit.NetCtx;
import com.jitlogic.netkit.NetException;
import com.jitlogic.netkit.http.*;
import com.jitlogic.netkit.log.Logger;
import com.jitlogic.netkit.log.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import static clojure.lang.Keyword.intern;
import static com.jitlogic.netkit.integ.ring.RingProtocol.*;

public class RingHandler implements HttpListener {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static ConcurrentMap<HttpMethod, Keyword> HTTP_METHODS = new ConcurrentHashMap<HttpMethod, Keyword>();

    private static Keyword getMethod(HttpMethod method) {
        if (method == HttpMethod.GET) return HTTP_GET;
        if (method == HttpMethod.POST) return HTTP_POST;

        Keyword m = HTTP_METHODS.get(method);
        if (m == null) {
            m = intern(method.toString().toLowerCase());
            HTTP_METHODS.put(method, m);
        }
        return m;
    }

    private ITransientMap request;
    private ITransientMap headers;
    private List<byte[]> bodyParts = new ArrayList<byte[]>();

    private IFn fn;
    private Executor executor;
    private HttpConfig config;

    public RingHandler(HttpConfig config, IFn fn, Executor executor) {
        this.fn = fn;
        this.config = config;
        this.executor = executor;
    }

    @Override
    public RingHandler request(SelectionKey key, String httpVersion, HttpMethod method, String uri, String query) {
        NetCtx ctx = NetCtx.fromKey(key);

        request = PersistentArrayMap.EMPTY.asTransient()
                .assoc(SERVER_PORT, ctx.getServerPort())
                .assoc(SERVER_NAME, ctx.getServerName())
                .assoc(REMOTE_ADDR, ctx.getRemoteAddr())
                .assoc(PROTOCOL, httpVersion)
                .assoc(URI, uri)
                .assoc(QUERY_STRING, query)
                .assoc(REQUEST_METHOD, getMethod(method))
                .assoc(SCHEME, ctx.getTlsContext() != null ? HTTPS : HTTP);

        headers = PersistentArrayMap.EMPTY.asTransient();

        // TODO SSL_CLIENT_CERT
        return this;
    }

    @Override
    public RingHandler response(SelectionKey key, String httpVersion, int status, String statusMessage) {
        throw new NetException("Should not happen.");
    }

    @Override
    public RingHandler header(SelectionKey key, String name, String value) {
        name = name.toLowerCase();
        Object v = headers.valAt(name);
        if (v == null) {
            headers = headers.assoc(name, value);
        } else {
            if (v.getClass() == String.class) {
                headers = headers.assoc(name, PersistentVector.create(v, value));
            } else if (v instanceof PersistentVector) {
                PersistentVector pv = (PersistentVector) v;
                headers = headers.assoc(name, pv.assoc(pv.size(), value));
            }
        }
        return this;
    }

    @Override
    public RingHandler body(SelectionKey key, Object... parts) {
        for (Object part : parts) {
            if (part instanceof ByteBuffer) {
                ByteBuffer bb = (ByteBuffer)part;
                if (bb.remaining() == 0) continue;
                byte[] b = new byte[bb.remaining()];
                bb.get(b);
                bodyParts.add(b);
            }
        }
        return this;
    }

    @Override
    public RingHandler finish(SelectionKey key) {
        if (!bodyParts.isEmpty()) {
            int sz = 0;
            for (byte[] b : bodyParts) {
                sz += b.length;
            }
            byte[] body = new byte[sz];
            int pos = 0;
            for (byte[] b : bodyParts) {
                System.arraycopy(b, 0, body, pos, b.length);
                pos += b.length;
            }
            // TODO implement streaming
            request = request.assoc(BODY_DATA, new ByteArrayInputStream(body));
            bodyParts.clear();
        }

        request = request.assoc(REQ_HEADERS, headers.persistent());
        RingTask task = new RingTask(key, fn, new HttpEncoder(config), request.persistent());
        executor.execute(task);
        return this;
    }

    @Override
    public RingHandler error(SelectionKey key, int status, String message, Object data, Throwable e) {
        log.error("Error: " + message + " '" + data + "'", e);
        HttpProtocolHandler.errorResponse(config, key, 500, message);
        return this;
    }
}
