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
package com.jitlogic.zorka.core;


import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.StringMatcher;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Date;
import java.util.List;
import java.util.Set;

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

}
