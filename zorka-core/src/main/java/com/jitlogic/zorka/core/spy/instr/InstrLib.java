package com.jitlogic.zorka.core.spy.instr;

import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.spy.SpyDefinition;
import com.jitlogic.zorka.core.spy.SpyLib;
import com.jitlogic.zorka.core.spy.TracerLib;


/**
 * Instrumentations (intrinsics): dedicated implementations of (fragments of) request processing chains for instrumenting
 * particular things (eg. HTTP client, SQL etc.). In many cases these are common parts of various categories of
 * instrumented code (eg. SQL APIs).
 */
public class InstrLib {

    private ZorkaConfig config;

    private SpyLib spyLib;
    private TracerLib tracerLib;


    public InstrLib(ZorkaConfig config, SpyLib spyLib, TracerLib tracerLib) {
        this.config = config;
        this.spyLib = spyLib;
        this.tracerLib = tracerLib;
    }


    public SpyDefinition[] jvmHttpClientInstr(String name) {
        JvmHttpClientInstrumentation inst = new JvmHttpClientInstrumentation(config, spyLib, tracerLib, name);
        return new SpyDefinition[] { inst.preSdef(), inst.callSdef() };
    }


}
