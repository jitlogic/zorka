package com.jitlogic.zorka.core.test.scripting;

import com.jitlogic.zorka.core.AgentInstance;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CatalinaBshUnitTest extends BshTestFixture {

    @Test
    public void testTraceBshBasic() {
        AgentInstance inst = instance();

        assertEquals("OK", inst.getZorkaAgent().loadScript("apache/catalina.bsh"));
    }

}
