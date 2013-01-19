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

package com.jitlogic.zorka.test;

import com.jitlogic.zorka.test.agent.BshAgentIntegTest;
import com.jitlogic.zorka.test.integ.NrpeAgentIntegTest;
import com.jitlogic.zorka.test.integ.SnmpIntegTest;
import com.jitlogic.zorka.test.integ.SyslogIntegTest;
import com.jitlogic.zorka.test.integ.ZabbixAgentIntegTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // agent
        BshAgentIntegTest.class,

        // integ
        NrpeAgentIntegTest.class, SnmpIntegTest.class, SyslogIntegTest.class, ZabbixAgentIntegTest.class,
})
public class IntegTestSuite {
}
