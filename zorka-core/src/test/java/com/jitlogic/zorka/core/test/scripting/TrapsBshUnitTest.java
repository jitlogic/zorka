package com.jitlogic.zorka.core.test.scripting;

import com.jitlogic.zorka.core.AgentInstance;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrapsBshUnitTest extends BshTestFixture {

    @Test
    public void testTraceBshDefault() {
        AgentInstance inst = instance();

        assertEquals("OK", inst.getZorkaAgent().loadScript("traps.bsh"));
    }

}
