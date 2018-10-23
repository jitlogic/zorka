package com.jitlogic.zorka.core.spy.instr;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.spy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLConnection;
import java.util.Map;

import static com.jitlogic.zorka.core.AgentConfigProps.TRACER_DISTRIBUTED_DEFV;
import static com.jitlogic.zorka.core.AgentConfigProps.TRACER_DISTRIBUTED_PROP;

public class JvmHttpClientInstrumentation {

    private static Logger log = LoggerFactory.getLogger(JvmHttpClientInstrumentation.class);

    private boolean distributed;
    private String name;
    private SpyLib spy;
    private TracerLib tracer;
    private long minTraceTime;

    private SpyProcessor procHdrIn;
    private SpyProcessor procHdrOut;

    private ThreadLocal<Map> reqPropsTL = new ThreadLocal<Map>();
    private ThreadLocal<String> methodTL = new ThreadLocal<String>();
    private ThreadLocal<String> dtraceUuidTL = new ThreadLocal<String>();
    private ThreadLocal<String> dtraceOutTL = new ThreadLocal<String>();


    public JvmHttpClientInstrumentation(ZorkaConfig config, SpyLib spyLib, TracerLib tracerLib, String name) {
        this.name = name;
        this.spy = spyLib;
        this.tracer = tracerLib;
        this.distributed = config.boolCfg(TRACER_DISTRIBUTED_PROP, TRACER_DISTRIBUTED_DEFV);
        this.minTraceTime = config.longCfg("httpclient.trace.time", tracer.getTracerMinTraceTime());
        this.procHdrIn = tracer.procAttr(TracerLib.PA_MAP_OF_LISTS_1, "HdrIn__", "THIS", "headerFields");
        this.procHdrOut = tracer.procAttr(TracerLib.PA_MAP_OF_LISTS_1, "HdrOut__", "REQ_HDRS");
    }

    public SpyDefinition preSdef() {
        return spy.instance(name + "_PRE")
                .onEnter(spy.fetchArg("THIS", 0), spy.fetchArg("METHOD", 1), new PreEnter());

    }

    public SpyDefinition callSdef() {
        return spy.instrument(name + "_CALL")
                .onEnter(spy.fetchArg("THIS", 0), new CallEnter())
                .onReturn(spy.fetchRetVal("STATUS"))
                .onError(spy.fetchError("ERR"), tracer.markError())
                .onSubmit(new CallSubmit());
    }

    public class PreEnter implements SpyProcessor {

        private SpyProcessor dto = tracer.dtraceOutput(true, false);

        @Override
        public Map<String, Object> process(Map<String, Object> rec) {

            Object obj = rec.get("THIS");

            if (!(obj instanceof URLConnection)) return rec;

            URLConnection conn = (URLConnection)obj;

            Object propsObj = ObjectInspector.get(conn, "requestProperties");

            methodTL.set((String)rec.get("METHOD"));

            if (propsObj instanceof Map) {

                // Request properties are accessible. It is possible to set distributed trace IDs.

                reqPropsTL.set((Map)propsObj);

            } else {
                reqPropsTL.remove();
                dtraceOutTL.remove();
                dtraceUuidTL.remove();
            }

            try {
                if (distributed && conn.getRequestProperty(TracerLib.DTRACE_UUID_HDR) == null) {
                    rec = dto.process(rec);
                    DTraceState ds = (DTraceState) rec.get("DTRACE");
                    String dtraceOut = rec.get(TracerLib.DTRACE_OUT).toString();

                    conn.addRequestProperty(TracerLib.DTRACE_UUID_HDR, ds.getUuid());
                    conn.addRequestProperty(TracerLib.DTRACE_TID_HDR, dtraceOut);

                    dtraceUuidTL.set(ds.getUuid());
                    dtraceOutTL.set(dtraceOut);

                    Object xtt = rec.get(TracerLib.DTRACE_XTT);
                    if (xtt != null) {
                        conn.addRequestProperty(TracerLib.DTRACE_XTT_HDR, xtt.toString());
                    }
                }
            } catch (IllegalStateException e) {
                log.debug("Skipping dtrace attributes setup due to IllegalStateException.");
                dtraceUuidTL.remove();
                dtraceOutTL.remove();
            }

            return rec;
        }
    }


    public class CallEnter implements SpyProcessor {

        @Override
        public Map<String, Object> process(Map<String, Object> rec) {

            if (methodTL.get() == null) return rec;

            Object obj = rec.get("THIS");

            if (!(obj instanceof URLConnection)) {
                return rec;
            }

            Object propsObj = ObjectInspector.get(obj, "requestProperties");

            if (propsObj == null) {
                propsObj = ObjectInspector.get(obj, ".requests", "headers");
            }

            if (propsObj == null) {
                propsObj = reqPropsTL.get();
            }

            reqPropsTL.remove();

            if (propsObj instanceof Map) {
                rec.put("REQ_HDRS", propsObj);
            }

            if (propsObj != null) {
                tracer.traceBegin("HTTP_CLI", minTraceTime);
            }

            return rec;
        }
    }


    public class CallSubmit implements SpyProcessor {

        @Override
        public Map<String, Object> process(Map<String, Object> rec) {

            String method = methodTL.get();
            methodTL.remove();

            if (method == null) return rec;

            String tid = dtraceOutTL.get();
            String uuid = dtraceUuidTL.get();

            if (tid != null && uuid != null) {
                tracer.newAttr(TracerLib.DTRACE_UUID, uuid);
                tracer.newAttr(TracerLib.DTRACE_OUT, uuid + tid);
            }

            dtraceOutTL.remove();
            dtraceUuidTL.remove();

            tracer.newAttr("URL", ObjectInspector.substitute("${THIS.URL}", rec));
            tracer.newAttr("URI", ObjectInspector.substitute("${THIS.URL.path}", rec));
            tracer.newAttr("STATUS", ObjectInspector.substitute("${STATUS}", rec));
            tracer.newAttr("METHOD", method);

            procHdrIn.process(rec);
            procHdrOut.process(rec);

            return rec;
        }
    }

}
