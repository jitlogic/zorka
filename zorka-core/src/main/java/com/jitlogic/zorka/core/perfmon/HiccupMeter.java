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

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class HiccupMeter implements Runnable {

    private static ZorkaLog log = ZorkaLogger.getLog(HiccupMeter.class);

    public static final long MS = 1000000L;

    private long resolution = 1, delay = 30000;
    private String path;
    private String threadName;

    private volatile boolean running;
    private volatile Thread thread;


    private volatile Long memTstamp = new Long(0);

    private boolean memEnabled, dskEnabled;

    private MethodCallStatistic stats;

    private long tstamp;


    public static HiccupMeter cpuMeter(long resolution, long delay, MethodCallStatistic stats) {
        HiccupMeter meter = new HiccupMeter(resolution, delay, stats);
        meter.threadName = "ZORKA-Hiccup-cpu";
        return meter;
    }


    public static HiccupMeter memMeter(long resolution, long delay, MethodCallStatistic stats) {
        HiccupMeter meter = new HiccupMeter(resolution, delay, stats);
        meter.memEnabled = true;
        meter.threadName = "ZORKA-Hiccup-mem";
        return meter;
    }


    public static HiccupMeter dskMeter(long resolution, long delay, String path, MethodCallStatistic stats) {
        HiccupMeter meter = new HiccupMeter(resolution, delay, stats);
        meter.dskEnabled = true;
        meter.path = path;
        meter.threadName = "ZORKA-Hiccup-dsk";
        return meter;
    }


    protected HiccupMeter(long resolution, long delay, MethodCallStatistic stats) {
        this.resolution = resolution;
        this.delay = delay;
        this.stats = stats;
    }


    private byte bt(long l, int offs) {
        return (byte)((l>>offs) & 0xff);
    }


    private void dskCycle(long t) {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(path);
            os.write(new byte[]{ bt(t,56), bt(t,48), bt(t,40), bt(t,32), bt(t,24), bt(t,16), bt(t,8), bt(t,0) });
            os.flush();
            os.getFD().sync();
        } catch (FileNotFoundException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot perform disk test", e);
        } catch (IOException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot perform disk test", e);
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException e) { }
            }
        }
    }

    private void memCycle(long t) {
        memTstamp = new Long(t - memTstamp.longValue());
    }


    public void cycle(long t) {
        if (memEnabled) {
            memCycle(t);
        }

        if (dskEnabled) {
            dskCycle(t);
        }

        if (tstamp != 0) {
            stats.logCall(Math.abs(t-tstamp-resolution*MS));
        }

        tstamp = t;
    }


    private void sleep() {
        try {
            Thread.sleep(resolution);
        } catch (InterruptedException e) {
            log.warn(ZorkaLogger.ZAG_WARNINGS, "Hiccup-meter thread encountered interruption. ");
        }
    }


    @Override
    public void run() {

        while (running) {
            cycle(System.nanoTime());
            sleep();
        }
    }


    public synchronized void start() {
        if (!running) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName("ZORKA-hiccup-meter");
            running = true;
            thread.start();
        }
    }


    public synchronized void stop() {
        running = false;
    }


    public MethodCallStatistic getStats() {
        return stats;
    }
}
