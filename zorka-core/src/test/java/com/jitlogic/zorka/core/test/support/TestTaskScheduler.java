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
