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
package com.jitlogic.zorka.core;


import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.StringMatcher;
import com.jitlogic.zorka.common.util.TapInputStream;
import com.jitlogic.zorka.common.util.TapOutputStream;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class UtilLib {

    /**
     * Recursively accesses object. This is just a ObjectInspector.get() method facade for configuration scripts.
     *
     * @param obj  source object
     * @param args attribute chain
     * @return retrieved value
     */
    public Object get(Object obj, Object... args) {
        return ObjectInspector.get(obj, args);
    }


    public List<?> list(Object...objs) {
        List<Object> lst = new ArrayList<Object>();
        for (Object obj : objs) {
            lst.add(obj);
        }
        return lst;
    }


    public Map<?,?> map(Object...objs) {
        return ZorkaUtil.map(objs);
    }


    public Set<Object> set(Object... objs) {
        return ZorkaUtil.set(objs);
    }


    public String castString(Object obj) {
        return ZorkaUtil.castString(obj);
    }


    public String crc32sum(String input) {
        return ZorkaUtil.crc32(input);
    }


    public String crc32sum(String input, int limit) {
        String sum = ZorkaUtil.crc32(input);
        return sum.length() > limit ? sum.substring(0, limit) : sum;
    }


    public String md5sum(String input) {
        return ZorkaUtil.md5(input);
    }


    public String md5sum(String input, int limit) {
        String sum = ZorkaUtil.md5(input);
        return sum.length() > limit ? sum.substring(0, limit) : sum;
    }


    public String sha1sum(String input) {
        return ZorkaUtil.sha1(input);
    }


    public String sha1sum(String input, int limit) {
        String sum = ZorkaUtil.sha1(input);
        return sum.length() > limit ? sum.substring(0, limit) : sum;
    }


    public String strTime(long ns) {
        return ZorkaUtil.strTime(ns);
    }


    public String strClock(long clock) {
        return ZorkaUtil.strClock(clock);
    }


    public StringMatcher stringMatcher(List<String> includes, List<String> excludes) {
        return new StringMatcher(includes, excludes);
    }

    public String path(String... components) {
        return ZorkaUtil.path(components);
    }

    public Object getField(Object obj, String name) {
        return ObjectInspector.getField(obj, name);
    }


    public void setField(Object obj, String name, Object value) {
        ObjectInspector.setField(obj, name, value);
    }

    public String json(Object obj) {
        return new JSONWriter().write(obj);
    }

    public Pattern rePattern(String pattern) {
        return Pattern.compile(pattern);
    }

    public boolean reMatch(Pattern pattern, String s) {
        return pattern.matcher(s).matches();
    }

    private static final int BUF_SZ = 4096;

    public int ioCopy(InputStream is, OutputStream os) throws IOException {
        int bufsz = Math.min(is.available(), BUF_SZ);
        final byte[] buf = new byte[bufsz];
        int n = 0, total = 0;
        n = is.read(buf);
        while (n > 0) {
            os.write(buf, 0, n);
            total += n;
            n = is.read(buf);
        }
        return total;
    }

    public long min(long a, long b) {
        return Math.min(a, b);
    }

    public long max(long a, long b) {
        return Math.max(a, b);
    }

    public byte[] clipArray(byte[] src, int len) {
        return ZorkaUtil.clipArray(src, len);
    }

    public String base64(byte[] buf) {
        return Base64.encode(buf, false);
    }

    public TapInputStream tapInputStream(InputStream is, long init, long limit) {
        return new TapInputStream(is, (int)init, (int)limit);
    }

    public TapOutputStream tapOutputStream(OutputStream os, long init, long limit) {
        return new TapOutputStream(os, (int)init, (int)limit);
    }
}
