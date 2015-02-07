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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Allows using ThreadLocal objects to transfer data across instrumented methods.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ThreadLocalProcessor implements SpyProcessor {

    /**
     * Command that calls ThreadLocal.get() method.
     */
    public static final int GET = 1;

    /**
     * Command that calls ThreadLocal.set() method.
     */
    public static final int SET = 2;

    /**
     * Command that calls ThreadLocal.remove() method.
     */
    public static final int REMOVE = 3;

    /**
     * Record field that will be used to transfer value between record and thread local
     */
    private String field;

    /**
     * Attribute chain - use if value accessed is reachable indirectly from spy record
     */
    private Object[] path;

    /**
     * Thread local operation (see above constants)
     */
    private int operation;

    /**
     * Thread local object
     */
    private ThreadLocal<Object> threadLocal;

    /**
     * Constructs thread local processor.
     *
     * @param field       record field that will be accessed
     * @param operation   thread local operation
     * @param threadLocal thread local object
     * @param path        attribute chain
     */
    public ThreadLocalProcessor(String field, int operation, ThreadLocal<Object> threadLocal, Object... path) {
        this.field = field;
        this.operation = operation;
        this.threadLocal = threadLocal;
        this.path = ZorkaUtil.copyArray(path);
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        switch (operation) {
            case GET:
                record.put(field, ObjectInspector.get(threadLocal.get(), path));
                break;
            case SET:
                threadLocal.set(record.get(field));
                break;
            case REMOVE:
                threadLocal.remove();
                break;
        }

        return record;
    }
}
