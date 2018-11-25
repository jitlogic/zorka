package com.jitlogic.netkit.test.support;

import clojure.lang.AFn;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.PersistentArrayMap;
import com.jitlogic.netkit.integ.ring.RingProtocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TestRingFn extends AFn {

    private List<IPersistentMap> reqs = new ArrayList<IPersistentMap>();
    private LinkedList<IPersistentMap> resps = new LinkedList<IPersistentMap>();

    public static IPersistentMap resp(long status, Object...args) {
        ITransientMap rslt = PersistentArrayMap.EMPTY.asTransient()
            .assoc(RingProtocol.STATUS, status);

        ITransientMap headers = PersistentArrayMap.EMPTY.asTransient();

        for (int i = 1; i < args.length; i+=2) {
            headers = headers.assoc(args[i-1].toString().toLowerCase(), args[i]);
        }

        rslt = rslt.assoc(RingProtocol.REQ_HEADERS, headers.persistent());

        if ((args.length % 2) != 0) {
            rslt = rslt.assoc(RingProtocol.BODY_DATA, args[args.length-1]);
        }

        return rslt.persistent();
    }

    public Object invoke(Object req) {
        reqs.add((IPersistentMap)req);
        return resps.pollFirst();
    }

    public List<IPersistentMap> getReqs() {
        return reqs;
    }

    public void add(IPersistentMap...resps) {
        this.resps.addAll(Arrays.asList(resps));
    }
}
