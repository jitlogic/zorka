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

package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;

public class CallingBshCollector implements SpyProcessor {

    private SpyProcessor collector = null;
    private String ns;

    public CallingBshCollector(String ns) {
        this.ns = ns;
    }

    public SpyRecord process(int stage, SpyRecord record) {
        if (collector == null) {
            ZorkaBshAgent agent = AgentInstance.instance().getZorkaAgent();
            collector = (SpyProcessor)agent.eval(
                    "(com.jitlogic.zorka.spy.SpyProcessor)"+ns);
        }

        collector.process(SpyLib.ON_COLLECT, record);

        return record;
    }
}
