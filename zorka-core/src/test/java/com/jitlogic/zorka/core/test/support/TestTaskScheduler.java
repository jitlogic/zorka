/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.core.Labeled;
import com.jitlogic.zorka.core.util.TaskScheduler;

import java.util.ArrayList;
import java.util.List;

public class TestTaskScheduler extends TaskScheduler {

    public static TestTaskScheduler instance() {
        instance = new TestTaskScheduler();
        return (TestTaskScheduler)instance;
    }

    private List<Runnable> scheduled = new ArrayList<Runnable>();

    public void schedule(Runnable runnable, long interval, long delay) {
        scheduled.add(runnable);
    }

    /**
     * Runs selected runnables.
     * @param filter either class name or class name and label split with ':'
     */
    public void runCycle(String filter) {
        String[] segs = filter.split(":");
        String className = segs[0], label = segs.length > 1 ? segs[1] : null;

        for (Runnable r : scheduled) {
            if (!"*".equals(className) && !r.getClass().getName().equals(className)) {
                continue;
            }
            if (label != null && !(r instanceof Labeled && label.equals(((Labeled)r).getLabel()))) {
                continue;
            }
            r.run();
        }
    }

}
