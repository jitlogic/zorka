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

package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;

public class ThreadLocalProcessor implements SpyProcessor {

    public final static int GET = 1;
    public final static int SET = 2;
    public final static int REMOVE = 3;

    private int slot, operation;
    private ThreadLocal<Object> threadLocal;


    public ThreadLocalProcessor(int slot, int operation, ThreadLocal<Object> threadLocal) {
        this.slot = slot;
        this.operation = operation;
        this.threadLocal = threadLocal;
    }


    public SpyRecord process(int stage, SpyRecord record) {
        switch (operation) {
            case GET:
                record.put(stage, slot, threadLocal.get());
                break;
            case SET:
                threadLocal.set(record.get(stage, slot));
                break;
            case REMOVE:
                threadLocal.remove();
                break;
        }

        return record;
    }
}
