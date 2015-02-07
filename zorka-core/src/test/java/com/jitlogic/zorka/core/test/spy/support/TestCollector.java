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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.spy.support;

import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCollector implements SpyProcessor {

    private List<Map<String,Object>> records = new ArrayList<Map<String,Object>>();

    public Map<String,Object> process(Map<String,Object> record) {
        records.add(record);
        return record;
    }

    public int size() {
        return records.size();
    }

    public Map<String,Object> get(int i) {
        return records.get(i);
    }

    public void start() {

    }

    public void stop() {

    }
}
