package com.jitlogic.zorka.core.test.scripting;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.AgentInstance;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpringBshUnitTest extends BshTestFixture {

    @Test
    public void testSpringBshBasic() {
        checkLoadScript("spring.bsh");
    }

}
