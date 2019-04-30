package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.common.tracedata.DTraceContext;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

/** Zipkin/JSON distributed trace data formatter. */
public class DTraceFormatterZJ implements DTraceFormatter {

    private static final Logger log = LoggerFactory.getLogger(DTraceFormatterZJ.class);

    private SymbolRegistry symbols;
    private Map<Integer,String> tagMap;
    private Set<Integer> tagExclusions;

    private int remoteIpAttr, remotePortAttr, localIpAttr, localPortAttr;

    public DTraceFormatterZJ(SymbolRegistry symbols, Map<String,String> tagMap) {
        this.symbols = symbols;
        this.tagMap = new HashMap<Integer, String>();

        for (Map.Entry<String,String> e : tagMap.entrySet()) {
            this.tagMap.put(symbols.symbolId(e.getKey()), e.getValue());
        }

        this.remoteIpAttr = symbols.symbolId("REMOTE_IP");
        this.remotePortAttr = symbols.symbolId("REMOTE_PORT");
        this.localIpAttr = symbols.symbolId("LOCAL_IP");
        this.localPortAttr = symbols.symbolId("LOCAL_PORT");

        tagExclusions = ZorkaUtil.constSet(remoteIpAttr, remotePortAttr, localIpAttr, localPortAttr);
    }

    private String getKind(int flags) {
        switch (flags & DFK_MASK) {
            case DFK_CLIENT: return "CLIENT";
            case DFK_SERVER: return "SERVER";
            case DFK_PRODUCER: return "PRODUCER";
            case DFK_CONSUMER: return "CONSUMER";
            default: return null;
        }
    }

    private byte[] format(TraceRecord tr) {
        try {
            DTraceContext ds = tr.getDTraceState();
            if (tr.getTraceId() == 0 || ds == null) return null;

            Map<String, Object> span = ZorkaUtil.map("id", ds.getSpanIdHex(),
                    "traceId", ds.getTraceIdHex(),
                    "name", symbols.symbolName(tr.getTraceId()),
                    "timestamp", ds.getTstart() * 1000,
                    "duration", tr.getTime() / 1000
            );

            String kind = getKind(ds.getFlags());
            if (kind != null) span.put("kind", kind);

            if (ds.getParentId() != 0) span.put("parentId", ds.getParentIdHex());
            if (ds.hasFlags(F_DEBUG)) span.put("debug", true);

            // Remote endpoint
            String remoteIp = (String) tr.getAttr(remoteIpAttr);
            Integer remotePort = ZorkaUtil.lcastInt(tr.getAttr(remotePortAttr));
            if (remoteIp != null || remotePort != null) {
                Map<String, Object> re = new TreeMap<String, Object>();
                if (remoteIp != null) re.put("ipv4", remoteIp);
                if (remotePort != null) re.put("port", remotePort);
                span.put("remoteEndpoint", re);
            }

            // Local endpoint
            String localIp = (String) tr.getAttr(localIpAttr);
            Integer localPort = ZorkaUtil.lcastInt(tr.getAttr(localPortAttr));
            if (localIp != null || localPort != null) {
                Map<String, Object> le = new TreeMap<String, Object>();
                if (localIp != null) le.put("ipv4", remoteIp);
                if (localPort != null) le.put("port", remotePort);
                span.put("localEndpoint", le);
            }

            // Annotations (only error annotation is used at the moment)
            if (tr.getMarker() != null && tr.getMarker().hasFlag(TraceMarker.ERROR_MARK)) {
                span.put("annotations", Collections.singletonList(
                        ZorkaUtil.map("timestamp", ds.getTstart() * 1000, "error")));
            }

            // Additional tags
            Map<String, String> tags = new TreeMap<String, String>();
            for (Map.Entry<Integer, String> e : tagMap.entrySet()) {
                if (!tagExclusions.contains(e.getKey())) {
                    Object v = tr.getAttr(e.getKey());
                    if (v != null) {
                        tags.put(e.getValue(), v.toString());
                    }
                }
            }

            if (tags.size() > 0) span.put("tags", tags);

            return new JSONWriter().write(span).getBytes();
        } catch (Exception e) {
            log.error("Cannot format trace record " + tr, e);
        }
        return null;
    }

    @Override
    public byte[] format(List<TraceRecord> acc, int softLimit, int hardLimit) {
        List<byte[]> fragments = new ArrayList<byte[]>(acc.size());
        int size = 1;

        while (size < softLimit && acc.size() > 0) {
            byte[] b = format(acc.get(0));
            acc.remove(0);
            if (b == null) continue;
            if (size + b.length + 1 > hardLimit) break;
            fragments.add(b);
            size += b.length + 1;
        }

        if (fragments.size() == 0) return null;

        byte[] buf = new byte[size];
        buf[0] = (byte)'[';
        int pos = 1;
        for (int i = 0; i < fragments.size(); i++) {
            byte[] b = fragments.get(i);
            System.arraycopy(b, 0, buf, pos, b.length);
            pos += b.length;
            buf[pos] = i < fragments.size()-1 ? (byte)',' : (byte)']';
        }

        return buf;
    }
}
