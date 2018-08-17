/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.core.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;


public class DummySpyRetransformer implements SpyRetransformer {

    private static final Logger log = LoggerFactory.getLogger(DummySpyRetransformer.class);

    public DummySpyRetransformer(Instrumentation instrumentation, AgentConfig config) {
        log.info("Class retransform is not supported. Online reconfiguration will be crippled.");
    }

    @Override
    public boolean retransform(SpyMatcherSet oldSet, SpyMatcherSet newSet, boolean isSdef) {
        log.warn("Ignoring classes retransform due to lack of platform support.");
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Class[] getAllLoadedClasses() {
        // This is for Java 5 and testing, so it will be ignored
        return new Class[0];
    }

}
