/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy.old;


import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

public class ZorkaSpyLib {
	
	
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	
    private MBeanServerRegistry mbr;
	
	
	public ZorkaSpyLib(ZorkaBshAgent agent) {
        mbr = agent.getMBeanServerRegistry();
	}

}
