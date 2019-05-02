/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.test.support;



import org.slf4j.impl.MemoryTrapper;
import org.slf4j.impl.ZorkaLogLevel;
import org.slf4j.impl.ZorkaLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.fail;

public class CommonTestUtil {

    public static Properties loadProps(String path) throws IOException {
        Properties props = new Properties();

        InputStream is = null;
        try {
            is = new FileInputStream(path);
            props.load(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return props;
    }

    public static void saveProps(Properties props, String path) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(path);
            props.store(os, "Unit test");
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public static int cmd(String cmd) throws Exception {
        return Runtime.getRuntime().exec(cmd).waitFor();
    }

    public static byte[] cat(String path) throws IOException {
        File f = new File(path);
        byte[] buf = new byte[(int) f.length()];
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            is.read(buf);
            is.close();
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return buf;
    }

    public static List<File> find(File dir, String suffix, Set<String> ignore) {
        List<File> lst = new ArrayList<File>();

        for (String fn : dir.list()) {
            if (ignore.contains(fn)) {
                continue;
            }

            File f = new File(dir, fn);

            if (f.isFile() && fn.endsWith(suffix)) {
                lst.add(f);
            }

            if (f.isDirectory() && !f.getPath().startsWith(".")) {
                lst.addAll(find(f, suffix, ignore));
            }
        }

        return lst;
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    public static Properties setProps(Properties props, String... data) {

        for (int i = 1; i < data.length; i += 2) {
            props.setProperty(data[i - 1], data[i]);
        }

        return props;
    }

    public static void printLogs(ZorkaLogLevel level) {
        printLogs((MemoryTrapper)(ZorkaLoggerFactory.getInstance().getTrapper()), level);
    }

    public static void checkLogs(MemoryTrapper logs, ZorkaLogLevel level) {
        for (MemoryTrapper.TrapperMessage msg : logs.getAll()) {
            if (msg.getLogLevel().getPriority() >= level.getPriority()) {
                printLogs(logs, ZorkaLogLevel.DEBUG);
                fail("Bad log: " + msg);
            }
        }
    }

    public static void printLogs(MemoryTrapper logs, ZorkaLogLevel level) {
        for (MemoryTrapper.TrapperMessage msg : logs.getAll()) {
            if (msg.getLogLevel().getPriority() >= level.getPriority()) {
                System.out.println(msg);
                if (msg.getE() != null) {
                    msg.getE().printStackTrace(System.out);
                }
            }
        }
    }

}
