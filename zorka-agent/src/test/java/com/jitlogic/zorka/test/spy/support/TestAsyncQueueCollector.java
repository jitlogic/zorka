/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.test.spy.support;

import com.jitlogic.zorka.spy.AsyncQueueCollector;

import java.util.Map;

public class TestAsyncQueueCollector extends AsyncQueueCollector {

    public Map<String,Object> process(int stage, Map<String,Object> record) {
        doProcess(record);
        return record;
    }

    public void start() {

    }

}
