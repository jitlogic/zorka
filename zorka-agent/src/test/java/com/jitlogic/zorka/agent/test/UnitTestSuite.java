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
package com.jitlogic.zorka.agent.test;

import com.jitlogic.zorka.agent.test.agent.BshAgentUnitTest;
import com.jitlogic.zorka.agent.test.agent.LogConfigUnitTest;
import com.jitlogic.zorka.agent.test.agent.MBeanMappingUnitTest;
import com.jitlogic.zorka.agent.test.agent.ZorkaLibUnitTest;
import com.jitlogic.zorka.agent.test.integ.NagiosAgentUnitTest;
import com.jitlogic.zorka.agent.test.integ.ZabbixAgentUnitTest;
import com.jitlogic.zorka.agent.test.integ.ZabbixDiscoveryUnitTest;
import com.jitlogic.zorka.agent.test.normproc.LdapLexerUnitTest;
import com.jitlogic.zorka.agent.test.normproc.XqlLexerUnitTest;
import com.jitlogic.zorka.agent.test.normproc.XqlNormalizationUnitTest;
import com.jitlogic.zorka.agent.test.rankproc.*;
import com.jitlogic.zorka.agent.test.spy.*;
import com.jitlogic.zorka.agent.test.spy.TraceBuilderUnitTest;
import com.jitlogic.zorka.agent.test.spy.TraceEnrichmentUnitTest;
import com.jitlogic.zorka.agent.test.spy.TracerInstrumentationUnitTest;

import com.jitlogic.zorka.agent.test.common.ObjectInspectorUnitTest;
import com.jitlogic.zorka.agent.test.common.ZorkaUtilUnitTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // testinteg
        ZabbixAgentUnitTest.class,

        // testrank
        RankProcUnitTest.class, ThreadRankUnitTest.class,

        // testspy
        ArgProcessingUnitTest.class, BytecodeInstrumentationUnitTest.class, ClassMethodMatchingUnitTest.class,
        SpyLibFunctionsUnitTest.class, StandardCollectorsUnitTest.class, SubmissionDispatchUnitTest.class,
        ZorkaStatsCollectionUnitTest.class,

        // teststress
        SubmissionDispatchUnitTest.class,

        // tracer
        TracerInstrumentationUnitTest.class, TraceBuilderUnitTest.class, TraceEnrichmentUnitTest.class,

        // unittest
        AggregateCountingUnitTest.class, AverageRateCountingUnitTest.class, BshAgentUnitTest.class,
        LdapLexerUnitTest.class, MBeanMappingUnitTest.class, NagiosAgentUnitTest.class, ObjectInspectorUnitTest.class,
        SlidingWindowUnitTest.class, XqlLexerUnitTest.class, XqlNormalizationUnitTest.class, ZabbixAgentUnitTest.class,
        ZabbixDiscoveryUnitTest.class, ZorkaLibUnitTest.class, ZorkaUtilUnitTest.class, LogConfigUnitTest.class
})
public class UnitTestSuite {
}
