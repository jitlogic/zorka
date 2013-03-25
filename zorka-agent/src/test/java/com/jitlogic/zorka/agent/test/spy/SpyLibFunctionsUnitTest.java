/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.agent.test.spy;

import com.jitlogic.zorka.agent.test.support.ZorkaFixture;
import com.jitlogic.zorka.agent.spy.SpyContext;
import com.jitlogic.zorka.agent.spy.SpyDefinition;
import com.jitlogic.zorka.agent.spy.SpyLib;

import com.jitlogic.zorka.agent.spy.ZorkaStatsCollector;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpyLibFunctionsUnitTest extends ZorkaFixture {

    private SpyLib spyLib;

    @Before
    public void setUp() {
        spyLib = new SpyLib(agentInstance.getClassTransformer());
    }


    @Test
    public void testSpyInstrumentConvenienceFn1() {
        SpyDefinition sdef = spyLib.instrument("test", "test:type=MyStats", "stats", "${0}");

        assertEquals(2, sdef.getProcessors(SpyLib.ON_SUBMIT).size());
        assertEquals("${A0}", ((ZorkaStatsCollector)sdef.getProcessors(SpyLib.ON_SUBMIT).get(1)).getStatTemplate());
        assertEquals(2, sdef.getProbes(SpyLib.ON_ENTER).size());
        assertEquals("A0", sdef.getProbes(SpyLib.ON_ENTER).get(0).getDstField());
    }


    @Test
    public void testSpyInstrumentConvenienceFnWithActualRemap() {
        SpyDefinition sdef = spyLib.instrument("test", "test:type=MyStats", "stats", "${1}");

        assertEquals(2, sdef.getProcessors(SpyLib.ON_SUBMIT).size());
        assertEquals("${A1}", ((ZorkaStatsCollector)sdef.getProcessors(SpyLib.ON_SUBMIT).get(1)).getStatTemplate());
    }


    @Test
    public void testSpyInstrumentConvenienceFnWithSingleMultipartVar() {
        SpyDefinition sdef = spyLib.instrument("test", "test:type=MyStats", "stats", "${0.request.url}");

        assertEquals(2, sdef.getProcessors(SpyLib.ON_SUBMIT).size());
        assertEquals("${A0.request.url}", ((ZorkaStatsCollector)sdef.getProcessors(SpyLib.ON_SUBMIT).get(1)).getStatTemplate());
    }


    @Test
    public void testSpyInstrumentConvenienceFnWithNonTranslatedVar() {
        SpyDefinition sdef = spyLib.instrument("test", "test:type=MyStats", "stats", "${methodName}");

        assertEquals(2, sdef.getProcessors(SpyLib.ON_SUBMIT).size());
        assertEquals("${methodName}", ((ZorkaStatsCollector)sdef.getProcessors(SpyLib.ON_SUBMIT).get(1)).getStatTemplate());
    }


    @Test
    public void testCtxSubst() {
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.pkg.TClass", "testMethod", "()V", 1);

        assertEquals("some.pkg.TClass", ctx.subst("${className}"));
        assertEquals("some.pkg", ctx.subst("${packageName}"));
        assertEquals("TClass", ctx.subst("${shortClassName}"));
        assertEquals("testMethod", ctx.subst("${methodName}"));
    }

}
