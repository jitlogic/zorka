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
package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.test.perfmon.support.TestRankItem;
import com.jitlogic.zorka.core.test.perfmon.support.TestRankLister;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import com.jitlogic.zorka.core.perfmon.RankList;
import com.jitlogic.zorka.core.perfmon.RankLister;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class RankProcUnitTest extends ZorkaFixture {

    @Test
    public void testCheckForLazyInitialRank() {
        RankLister lister = new TestRankLister("t", "avg1", 5, 4, 3, 2, 1);
        RankList<TestRankItem> rank = new RankList<TestRankItem>(lister, 3, 0, 0, 100);

        assertEquals(0, rank.getNumReranks());
        assertEquals(3, rank.size());
        assertEquals(1, rank.getNumReranks());
    }

    @Test
    public void testCheckForProperRankOrdering() {
        RankLister lister = new TestRankLister("t", "avg1", 5, 4, 3, 2, 1);
        RankList<TestRankItem> rank = new RankList<TestRankItem>(lister, 3, 0, 0, 100);

        List<TestRankItem> items = rank.list();

        assertEquals(5.0, items.get(0).getAverage(0L, 0, 0), 0.001);
    }
}
