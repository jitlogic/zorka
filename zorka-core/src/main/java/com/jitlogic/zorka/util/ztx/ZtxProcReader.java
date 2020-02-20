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

package com.jitlogic.zorka.util.ztx;

import com.jitlogic.zorka.core.spy.tuner.AbstractZtxReader;

import java.util.*;

public class ZtxProcReader extends AbstractZtxReader {

    private NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> data;

    public ZtxProcReader(NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> data) {
        this.data = data;
    }

    @Override
    public void add(String p, String c, String m, String s) {
        NavigableMap<String,NavigableMap<String,NavigableSet<String>>> mp = data.get(p);
        if (mp == null) {
            mp = new TreeMap<String,NavigableMap<String,NavigableSet<String>>>();
            data.put(p,mp);
        }

        NavigableMap<String,NavigableSet<String>> mc = mp.get(c);
        if (mc == null) {
            mc = new TreeMap<String,NavigableSet<String>>();
            mp.put(c, mc);
        }

        NavigableSet<String> ms = mc.get(m);
        if (ms == null) {
            ms = new TreeSet<String>();
            mc.put(m, ms);
        }

        ms.add(s);
    }
}
