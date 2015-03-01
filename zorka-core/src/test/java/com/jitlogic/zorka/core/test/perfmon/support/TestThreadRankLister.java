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
package com.jitlogic.zorka.core.test.perfmon.support;

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.BucketAggregate;
import com.jitlogic.zorka.core.perfmon.ThreadRankInfo;
import com.jitlogic.zorka.core.perfmon.ThreadRankLister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestThreadRankLister extends ThreadRankLister {

    private Map<Long,ThreadRankInfo> testInfos = new HashMap<Long, ThreadRankInfo>();

    public TestThreadRankLister(MBeanServerRegistry registry) {
        super(registry);
    }

    public TestThreadRankLister clear() {
        testInfos.clear();
        return this;
    }

    public TestThreadRankLister feed(long tid, String tname, long tcpu, long tblock) {
        testInfos.put(tid, new ThreadRankInfo(tid, tname, tcpu * BucketAggregate.MS, tblock * BucketAggregate.MS));

        return this;
    }

    @Override
    protected List<ThreadRankInfo> rawList() {
        List<ThreadRankInfo> lst = new ArrayList<ThreadRankInfo>(testInfos.size()+2);

        for (Map.Entry<Long,ThreadRankInfo> e : testInfos.entrySet()) {
            lst.add(e.getValue());
        }

        return lst;
    }
}
