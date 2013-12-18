/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestUtil {

    public static Properties loadProps(String path) throws IOException {
        Properties props = new Properties();

        try (InputStream is = new FileInputStream(path)) {
            props.load(is);
        }

        return props;
    }

    public static void rmrf(String path) throws IOException {
        rmrf(new File(path));
    }


    public static void rmrf(File f) throws IOException {
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    rmrf(c);
                }
            }
            f.delete();
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
}
