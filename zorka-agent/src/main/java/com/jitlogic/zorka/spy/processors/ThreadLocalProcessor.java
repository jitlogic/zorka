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

import static com.jitlogic.zorka.spy.SpyLib.fs;

public class ThreadLocalProcessor implements SpyProcessor {

    public final static int GET = 1;
    public final static int SET = 2;
    public final static int REMOVE = 3;

    private Object[] path;
    private int islt, sslt, operation;
    private ThreadLocal<Object> threadLocal;

    private ObjectInspector inspector = new ObjectInspector();

    public ThreadLocalProcessor(int[] slot, int operation, ThreadLocal<Object> threadLocal, Object...path) {
        this.sslt = slot[0];
        this.islt = slot[1];
        this.operation = operation;
        this.threadLocal = threadLocal;
        this.path = ZorkaUtil.copyArray(path);
    }


    public SpyRecord process(int stage, SpyRecord record) {
        switch (operation) {
            case GET:
            {
                Object v = threadLocal.get();
                for (Object key : path) {
                    v = inspector.get(v, key);
                }
                record.put(fs(sslt, stage), islt, v);
            }
                break;
            case SET:
                threadLocal.set(record.get(fs(sslt, stage), islt));
                break;
            case REMOVE:
                threadLocal.remove();
                break;
        }

        return record;
    }
}
