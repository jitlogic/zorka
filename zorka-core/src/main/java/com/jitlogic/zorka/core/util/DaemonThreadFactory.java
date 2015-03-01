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

package com.jitlogic.zorka.core.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {

    private String namePrefix;
    private AtomicInteger counter;
    private ThreadFactory dtf = Executors.defaultThreadFactory();

    public DaemonThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
        this.counter = new AtomicInteger(0);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = dtf.newThread(r);
        t.setDaemon(true);
        t.setName(namePrefix + "-" + counter.incrementAndGet());
        return t;
    }
}
