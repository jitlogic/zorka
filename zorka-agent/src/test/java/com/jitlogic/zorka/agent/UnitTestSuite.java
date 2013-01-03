/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.agent;

import com.jitlogic.zorka.agent.testinteg.NrpeAgentIntegTest;
import com.jitlogic.zorka.agent.testinteg.SnmpIntegTest;
import com.jitlogic.zorka.agent.testinteg.SyslogIntegTest;
import com.jitlogic.zorka.agent.testrank.RankProcUnitTest;
import com.jitlogic.zorka.agent.testrank.RankZorkaStatsUnitTest;
import com.jitlogic.zorka.agent.testrank.ThreadRankUnitTest;
import com.jitlogic.zorka.agent.testspy.*;
import com.jitlogic.zorka.agent.unittest.*;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Suite.SuiteClasses({
        // testinteg
        BshAgentUnitTest.class, NrpeAgentIntegTest.class, SnmpIntegTest.class, SyslogIntegTest.class,
        ZabbixAgentUnitTest.class,

        // testrank
        RankProcUnitTest.class, RankZorkaStatsUnitTest.class, ThreadRankUnitTest.class,

        // testspy
        ArgProcessingUnitTest.class, BytecodeInstrumentationUnitTest.class, ClassMethodMatchingUnitTest.class,
        SpyInstanceIntegTest.class, SpyLibFunctionsUnitTest.class, StandardCollectorsUnitTest.class,
        SubmissionDispatchUnitTest.class, ZorkaStatsCollectionUnitTest.class,

        // teststress
        SubmissionDispatchUnitTest.class,

        // unittest
        AggregateCountingUnitTest.class, AverageRateCountingUnitTest.class, BshAgentUnitTest.class,
        LdapLexerUnitTest.class, MBeanMappingUnitTest.class, NagiosAgentUnitTest.class, ObjectInspectorUnitTest.class,
        SlidingWindowUnitTest.class, XqlLexerUnitTest.class, XqlNormalizationUnitTest.class, ZabbixAgentUnitTest.class,
        ZabbixDiscoveryUnitTest.class, ZorkaLibUnitTest.class, ZorkaUtilUnitTest.class
})
@Categories.IncludeCategory(UnitTests.class)
public class UnitTestSuite {
}
