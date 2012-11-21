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
package com.jitlogic.zorka.agent.testrank;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;

import com.jitlogic.zorka.rankproc.ThreadRankItem;
import com.jitlogic.zorka.rankproc.ThreadRankLister;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;



public class ThreadRankUnitTest extends ZorkaFixture {

    @Test
    public void testListSinglePass() {
        ThreadRankLister lister = new TestThreadRankLister()
                .feed(1, "Thread-1", 100, 0)
                .feed(2, "Thread-2", 100, 0);

        lister.runCycle(100);

        List<ThreadRankItem> items = lister.list();
        assertEquals(2, items.size());
    }


    @Test
    public void testTwoPassesAndCheckIfNonExistentThreadsAreRemoved() {
        TestThreadRankLister lister = new TestThreadRankLister()
                .feed(1, "Thread-1", 100, 0)
                .feed(2, "Thread-2", 100, 0);

        lister.runCycle(1000);

        lister.clear()
                .feed(1, "Thread-1", 100, 0)
                .feed(3, "Thread-3", 100, 0);

        lister.runCycle(2000);

        List<ThreadRankItem> items = lister.list();

        assertEquals(2, items.size());
    }


    @Test
    public void testTwoPassesAndCheckAverages() {
        TestThreadRankLister lister = new TestThreadRankLister().feed(1, "Thread-1", 1000, 500);
        lister.runCycle(1000);
        lister.clear().feed(1, "Thread-1", 2000, 1500);
        lister.runCycle(2000);

        ThreadRankItem item = lister.list().get(0);
        assertEquals(10.0, item.getAverage(0, 0), 0.001);
    }


    @Test
    public void testTwoPassesWithWindowShift() {
        TestThreadRankLister lister = new TestThreadRankLister().feed(1, "Thread-1", 6000, 0);
        lister.runCycle(1000);

        assertEquals(20.0, lister.list().get(0).getAverage(0,0), 0.001);

        lister.clear().feed(1, "Thread-1", 1500, 0);
        lister.runCycle(31000);

        assertEquals(5.0, lister.list().get(0).getAverage(0,0), 0.001);
        assertEquals(10.0, lister.list().get(0).getAverage(0,1), 0.001);
    }
}
