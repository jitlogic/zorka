/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.AgentConfig;

import java.lang.instrument.Instrumentation;


public class DummySpyRetransformer implements SpyRetransformer {

    private static final ZorkaLog log = ZorkaLogger.getLog(DummySpyRetransformer.class);

    public DummySpyRetransformer(Instrumentation instrumentation, AgentConfig config) {
        log.info(ZorkaLogger.ZSP_CONFIG, "Class retransform is not supported. Online reconfiguration will be crippled.");
    }

    @Override
    public boolean retransform(SpyMatcherSet oldSet, SpyMatcherSet newSet, boolean isSdef) {
        log.warn(ZorkaLogger.ZSP_CONFIG, "Ignoring classes retransform due to lack of platform support.");
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

}
