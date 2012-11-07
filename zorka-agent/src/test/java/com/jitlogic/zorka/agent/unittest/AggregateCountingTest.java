/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.rankproc.BucketAggregate;
import static com.jitlogic.zorka.rankproc.BucketAggregate.*;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AggregateCountingTest {

    @Test
    public void testBucketConstructAndCheckWindowsCalculation() {
        BucketAggregate bag = new BucketAggregate(SEC, 60, 5, 3);

        long[] windows = bag.getWindows();

        assertNotNull(windows);
        assertEquals(4, windows.length);
        assertEquals(1*SEC, windows[0]);
        assertEquals(900*SEC, windows[3]);
    }


    @Test
    public void testBucketWindowToStageExactCalculation() {
        BucketAggregate bag = new BucketAggregate(SEC, 60, 5, 3, 4, 6, 4);

        assertEquals(0, bag.getStage(1 * SEC));
        assertEquals(1, bag.getStage(60 * SEC));
        assertEquals(2, bag.getStage(300*SEC));
        assertEquals(3, bag.getStage(900 * SEC));
        assertEquals(4, bag.getStage(3600*SEC));
        assertEquals(5, bag.getStage(6 * 3600 * SEC));
        assertEquals(6, bag.getStage(24*3600*SEC));
    }


    @Test
    public void testBucketWindowToStageApproximateCalculation() {
        BucketAggregate bag = new BucketAggregate(SEC, 60, 5);

        assertEquals(0, bag.getStage(30 * SEC));
        assertEquals(1, bag.getStage(45 * SEC));
        assertEquals(1, bag.getStage(120*SEC));
        assertEquals(2, bag.getStage(550 * SEC));
        assertEquals(-1, bag.getStage(650*SEC));
    }


    @Test
    public void testBucketFeedSingleVal() {
        BucketAggregate bag = new BucketAggregate(1*SEC, 60, 5, 3, 4, 6, 4);

        bag.feed(SEC+SEC/2, 10);

        assertEquals(10L, bag.getTotal());
        assertEquals(SEC, bag.getStart());
    }

    @Test
    public void testBucketFeedSingleValAndCheckSecondStage() throws Exception {
        BucketAggregate bag = new BucketAggregate(SEC, 2, 3, 3);

        bag.feed(SEC/2, 10);

        assertEquals(10, bag.getDelta(0));
        assertEquals(0, bag.getDelta(1));
    }

    @Test
    public void testBucketFeedTwoValsAndCheckSecondStage() throws Exception {
        BucketAggregate bag = new BucketAggregate(SEC, 2, 3, 3);

        bag.feed(SEC/2, 10);
        bag.feed(SEC+SEC/2, 20);

        assertEquals(20, bag.getDelta(0));
        assertEquals(10, bag.getDelta(1));
    }

    @Test
    public void testBucketFeedAndCheckIfItSumsOnSecondStage() throws Exception {
        BucketAggregate bag = new BucketAggregate(SEC, 3, 3, 3);

        bag.feed(SEC/2, 10);
        bag.feed(SEC+SEC/2, 20);
        bag.feed(2*SEC+SEC/2, 30);

        assertEquals(30, bag.getDelta(0));
        assertEquals(30, bag.getDelta(1));
        assertEquals(0, bag.getDelta(2));
    }

    @Test
    public void testBucketFeedAndCheckIfAccumulatesProperly() throws Exception {
        BucketAggregate bag = new BucketAggregate(SEC, 2, 3);

        for (int i = 1; i < 8; i++) {
            bag.feed(i*SEC + SEC/2, 10);
        }

        assertEquals(10, bag.getDelta(0));
        assertEquals(20, bag.getDelta(1));
        assertEquals(40, bag.getDelta(2));
    }
}
