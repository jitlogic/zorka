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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Cleans up distributed tracing state for current thread.
 * Should be attached to SUBMIT chain where DTraceInputProcessor is attached at ENTER.
 */
public class DTraceCleanProcessor implements SpyProcessor {

    private ThreadLocal<DTraceState> dtraceLocal;

    public DTraceCleanProcessor(ThreadLocal<DTraceState> dtraceLocal) {
        this.dtraceLocal = dtraceLocal;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        dtraceLocal.remove();

        // TODO force trace submission if forceLocal == true

        return rec;
    }
}
