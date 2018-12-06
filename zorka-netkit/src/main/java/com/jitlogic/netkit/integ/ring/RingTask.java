package com.jitlogic.netkit.integ.ring;

import clojure.lang.*;
import com.jitlogic.netkit.ArgumentException;
import com.jitlogic.netkit.http.HttpListener;
import com.jitlogic.netkit.http.HttpProtocol;
import com.jitlogic.netkit.log.Logger;
import com.jitlogic.netkit.log.LoggerFactory;
import com.jitlogic.netkit.util.NetkitUtil;

import java.nio.channels.SelectionKey;

import static com.jitlogic.netkit.integ.ring.RingProtocol.*;
import static com.jitlogic.netkit.util.TextUtil.camelCase;

public class RingTask implements Runnable {

    private Logger log = LoggerFactory.getLogger(RingTask.class);

    public IFn fn;
    private SelectionKey key;
    private HttpListener output;
    private IPersistentMap req;

    public RingTask(SelectionKey key, IFn fn, HttpListener output, IPersistentMap req) {
        this.key = key;
        this.fn = fn;
        this.output = output;
        this.req = req;
    }

    public IPersistentMap invoke(IPersistentMap req) {
        Object resp = fn.invoke(req);
        if (resp instanceof IPersistentMap) {
            return (IPersistentMap) resp;
        } else {
            return PersistentArrayMap.EMPTY.asTransient()
                    .assoc(STATUS, 200)
                    .assoc(BODY_DATA, resp)
                    .assoc(REQ_HEADERS, PersistentArrayMap.EMPTY)
                    .persistent();
        }

    }

    @Override
    public void run() {
        try {
            IPersistentMap resp = invoke(req);
            int status = 200;
            Object obj = resp.valAt(STATUS);
            if (obj != null) {
                if (obj.getClass() == Integer.class) {
                    status = (Integer)obj;
                } else if (obj.getClass() == Long.class) {
                    status = ((Long)obj).intValue();
                }
            }
            output.response(key, HttpProtocol.HTTP_1_1, status, null);

            IPersistentMap hdrs = (IPersistentMap)resp.valAt(REQ_HEADERS);
            if (hdrs != null) {
                for (Object kv : hdrs) {
                    String name;
                    Object val;
                    if (kv instanceof ISeq) {
                        ISeq seq = (ISeq) kv;
                        name = camelCase((String) seq.first());
                        val = seq.next().first();
                    } else if (kv instanceof MapEntry) {
                        MapEntry me = (MapEntry)kv;
                        name = me.key().toString();
                        val = me.val();
                    } else {
                        throw new ArgumentException("Map element of invalid datatype: " + kv.getClass());
                    }
                    if (val instanceof String) {
                        output.header(key, name, (String)val);
                    } else if (val instanceof ISeq) {
                        for (ISeq x = (ISeq)val; x != null; x = x.next()) {
                            if (x.first() != null)
                                output.header(key, name, x.first().toString());
                        }
                    }
                }
            }

            Object body = resp.valAt(BODY_DATA);
            if (body != null) output.body(key, NetkitUtil.toByteBuffer(body));
            output.finish(key);
        } catch (Exception e) {
            log.error("Error handling request: " + req, e);
            output.error(key, 500, "internal error", req, e);
        }
    }
}
