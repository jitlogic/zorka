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

import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Cleans up distributed tracing state for current thread.
 * Should be attached to SUBMIT chain where DTraceInputProcessor is attached at ENTER.
 */
public class DTraceCleanProcessor implements SpyProcessor {

    private ThreadLocal<String> uuidLocal;
    private ThreadLocal<String> tidLocal;
    private ThreadLocal<Boolean> forceLocal;

    public DTraceCleanProcessor(ThreadLocal<String> uuidLocal, ThreadLocal<String> tidLocal, ThreadLocal<Boolean> forceLocal) {
        this.uuidLocal = uuidLocal;
        this.tidLocal = tidLocal;
        this.forceLocal = forceLocal;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        uuidLocal.remove();
        tidLocal.remove();
        forceLocal.remove();

        // TODO force trace submission if forceLocal == true

        return rec;
    }
}