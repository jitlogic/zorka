/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.tuner;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractZtxReader {

    private static final Pattern RE_EMPTY = Pattern.compile("^\\s*$");
    private static final Pattern RE_COMMENT = Pattern.compile("^\\s*#.*");
    private static final Pattern RE_SINGLE = Pattern.compile("^([^|]*)\\|([^|]+)\\|([^|]+)\\|([^|]+)");
    private static final Pattern RE_PKG = Pattern.compile("^([^ |]+)$");
    private static final Pattern RE_CLS = Pattern.compile("^ ([^ |]+)$");
    private static final Pattern RE_MET = Pattern.compile("^  ([^ |]+)$");
    private static final Pattern RE_MSIG = Pattern.compile("^  ([^ |]+)\\|([^|]+)$");
    private static final Pattern RE_SIG =  Pattern.compile("^   ([^|]+)$");


    public void read(String path) throws IOException {
        read(new File(path));
    }

    public void read(File path) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(path);
            read(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public void read(InputStream is) throws IOException {
        if (is == null) throw new IOException("Null stream passed.");

        BufferedReader rdr = new BufferedReader(new InputStreamReader(is));

        String pkg = null;
        String cls = null;
        String met = null;

        int ln = 1;

        for (String l = rdr.readLine(); l != null; l = rdr.readLine(),ln++) {
            Matcher m;

            if (RE_COMMENT.matcher(l).matches()) continue;

            if (RE_EMPTY.matcher(l).matches()) {
                pkg = "";
                continue;
            }

            m = RE_SINGLE.matcher(l);
            if (m.matches()) {
                add(m.group(1), m.group(2), m.group(3), m.group(4));
                pkg = cls = met = null;
                continue;
            }

            m = RE_PKG.matcher(l);
            if (m.matches()) {
                pkg = m.group(1);
                cls = met = null;
                continue;
            }

            m = RE_CLS.matcher(l);
            if (m.matches()) {
                if (pkg == null) throw new IOException("Package not declared at line: " + ln);
                cls = m.group(1);
                met = null;
                continue;
            }

            m = RE_MSIG.matcher(l);
            if (m.matches()) {
                if (pkg == null || cls == null) throw new IOException("Package or class not declared at line: " + ln);
                add(pkg, cls, m.group(1), m.group(2));
                continue;
            }

            m = RE_MET.matcher(l);
            if (m.matches()) {
                if (pkg == null || cls == null) throw new IOException("Package or class not declared at line: " + ln);
                met = m.group(1);
                continue;
            }

            m = RE_SIG.matcher(l);
            if (m.matches()) {
                if (pkg == null || cls == null || met == null) throw new IOException("Package class or method not declared at line: " + ln);
                add(pkg, cls, met, m.group(1));
                continue;
            }

            throw new IOException("Malformed line " + ln + ": '" + l + "'");
        }
    }

    public abstract void add(String p, String c, String m, String s);
}
