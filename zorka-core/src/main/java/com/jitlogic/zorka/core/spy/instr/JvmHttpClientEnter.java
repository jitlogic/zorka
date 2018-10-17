package com.jitlogic.zorka.core.spy.instr;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.TracerLib;

import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.core.AgentConfigProps.TRACER_DISTRIBUTED_DEFV;
import static com.jitlogic.zorka.core.AgentConfigProps.TRACER_DISTRIBUTED_PROP;

public class JvmHttpClientEnter implements SpyProcessor {

    private boolean distributed;
    private TracerLib tracerLib;
    private SpyProcessor dtraceOutput;

    public JvmHttpClientEnter(ZorkaConfig config, TracerLib tracerLib) {
        this.distributed = config.boolCfg(TRACER_DISTRIBUTED_PROP, TRACER_DISTRIBUTED_DEFV);
        this.tracerLib = tracerLib;
        this.dtraceOutput = tracerLib.dtraceOutput();
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        Object obj = rec.get("THIS");

        if (!(obj instanceof URLConnection)) {
            return rec;
        }

        URLConnection conn = (URLConnection)obj;

        Object propsObj = ObjectInspector.get(conn, "requestProperties");

        if (propsObj instanceof Map) {
            rec.put("REQ_HDRS", propsObj);

            Map<String,List<String>> props = (Map<String,List<String>>)propsObj;

            if (distributed && !props.containsKey(TracerLib.DTRACE_UUID_HDR)) {
                rec = dtraceOutput.process(rec);
                DTraceState ds = (DTraceState)rec.get("DTRACE");
                conn.addRequestProperty(TracerLib.DTRACE_UUID_HDR, ds.getUuid());
                conn.addRequestProperty(TracerLib.DTRACE_TID_HDR, rec.get(TracerLib.DTRACE_OUT).toString());
                Object xtt = rec.get(TracerLib.DTRACE_XTT);
                if (xtt != null) {
                    conn.addRequestProperty(TracerLib.DTRACE_XTT_HDR, xtt.toString());
                }
            }
        }


        return rec;
    }
}
