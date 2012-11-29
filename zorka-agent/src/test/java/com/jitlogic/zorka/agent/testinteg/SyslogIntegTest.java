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
package com.jitlogic.zorka.agent.testinteg;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.integ.syslog.SyslogLib;
import com.jitlogic.zorka.integ.syslog.SyslogLogger;
import org.junit.Test;

public class SyslogIntegTest extends ZorkaFixture {

    @Test
    public void testTrivialSyslog() throws Exception{
        SyslogLogger logger = syslogLib.get("test", "127.0.0.1", "test");
        logger.log(SyslogLib.S_ERROR, SyslogLib.F_LOCAL5, "test", "Some test message.");
        Thread.sleep(10);
    }

}
