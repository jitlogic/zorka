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
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaUtil;

public class ThreadLocalProcessor implements SpyProcessor {

    public static final int GET = 1;
    public static final int SET = 2;
    public static final int REMOVE = 3;

    private String key;
    private Object[] path;
    private int operation;
    private ThreadLocal<Object> threadLocal;

    private ObjectInspector inspector = new ObjectInspector();

    public ThreadLocalProcessor(String key, int operation, ThreadLocal<Object> threadLocal, Object...path) {
        this.key = key;
        this.operation = operation;
        this.threadLocal = threadLocal;
        this.path = ZorkaUtil.copyArray(path);
    }


    public SpyRecord process(SpyRecord record) {
        switch (operation) {
            case GET:
                record.put(key, inspector.get(threadLocal.get(), path));
                break;
            case SET:
                threadLocal.set(record.get(key));
                break;
            case REMOVE:
                threadLocal.remove();
                break;
        }

        return record;
    }
}
