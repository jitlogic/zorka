package com.jitlogic.netkit.log;

import java.io.PrintStream;
import java.util.Date;

public class ConsoleLogger implements LoggerOutput {

    private PrintStream out;

    public ConsoleLogger(int level, PrintStream out) {
        this.out = out;
    }

    @Override
    public void log(int level, String tag, String msg, Throwable e) {
        out.println(String.format("%s %s [%s] %s - %s", new Date(), LoggerFactory.LEVELS.get(level), tag,
                Thread.currentThread().getName(), msg));
        if (e != null) e.printStackTrace(out);
    }

}
