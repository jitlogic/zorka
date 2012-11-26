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

import com.jitlogic.zorka.rankproc.CircularBucketAggregate;
import com.jitlogic.zorka.rankproc.SlidingBucketAggregate;
import org.junit.Test;
import static org.junit.Assert.*;

public class AggregateCountingTest {

    @Test
    public void testBucketConstructAndCheckWindowsCalculation() {
        BucketAggregate bag = new SlidingBucketAggregate(SEC, 60, 5, 3);

        assertEquals(4, bag.size());
        assertEquals(1*SEC, bag.getWindow(0));
        assertEquals(900*SEC, bag.getWindow(3));
    }


    @Test
    public void testBucketWindowToStageExactCalculation() {
        BucketAggregate bag = new SlidingBucketAggregate(SEC, 60, 5, 3, 4, 6, 4);

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
        BucketAggregate bag = new SlidingBucketAggregate(SEC, 60, 5);

        assertEquals(0, bag.getStage(30 * SEC));
        assertEquals(1, bag.getStage(45 * SEC));
        assertEquals(1, bag.getStage(120*SEC));
        assertEquals(2, bag.getStage(550 * SEC));
        assertEquals(-1, bag.getStage(650*SEC));
    }


    @Test
    public void testBucketFeedSingleVal() {
        BucketAggregate bag = new SlidingBucketAggregate(1*SEC, 60, 5, 3, 4, 6, 4);

        bag.feed(SEC+SEC/2, 10);

        assertEquals(10L, bag.getTotal());
        assertEquals(SEC, bag.getStart());
    }

    @Test
    public void testBucketFeedSingleValAndCheckSecondStage() throws Exception {
        BucketAggregate bag = new SlidingBucketAggregate(SEC, 2, 3, 3);

        bag.feed(SEC/2, 10);

        assertEquals(10, bag.getDeltaV(0L, 0));
        assertEquals(0, bag.getDeltaV(0L, 1));
    }

    @Test
    public void testBucketFeedTwoValsAndCheckSecondStage() throws Exception {
        BucketAggregate bag = new SlidingBucketAggregate(SEC, 2, 3, 3);

        bag.feed(SEC/2, 10);
        bag.feed(SEC + SEC / 2, 20);

        assertEquals(20, bag.getDeltaV(0L, 0));
        assertEquals(10, bag.getDeltaV(0L, 1));
    }

    @Test
    public void testBucketFeedAndCheckIfItSumsOnSecondStage() throws Exception {
        BucketAggregate bag = new SlidingBucketAggregate(SEC, 3, 3, 3);

        bag.feed(SEC/2, 10);
        bag.feed(SEC+SEC/2, 20);
        bag.feed(2*SEC+SEC/2, 30);

        assertEquals(30, bag.getDeltaV(0L, 0));
        assertEquals(30, bag.getDeltaV(0L, 1));
        assertEquals(0, bag.getDeltaV(0L, 2));
    }

    @Test
    public void testSlidingAccumulation1() throws Exception {
        checkDeltas(makeAndFeedSl(7, 2, 3), 10, 20, 40);
    }

    @Test
    public void testSlidingAccumulation2() throws Exception {
        checkDeltas(makeAndFeedSl(8, 2, 3, 3), 10, 20, 50, 0);
    }

    @Test
    public void testSlidingAccumulation3() throws Exception {
        checkDeltas(makeAndFeedSl(9, 2, 3, 3), 10, 20, 50, 10);
    }

    @Test
    public void testSlidingAccumulation4() throws Exception {
        checkDeltas(makeAndFeedSl(10, 2, 3, 3), 10, 20, 60, 10);
    }

    @Test
    public void testSlidingAccumulation5() throws Exception {
        checkDeltas(makeAndFeedSl(11, 2, 3, 3), 10, 20, 50, 30);
    }


    @Test
    public void testCircularAccumulation1() {
        BucketAggregate bag = new CircularBucketAggregate(1000, 10, 60, 300, 900);

        bag.feed(10000, 10);
        bag.feed(11000, 10);
        bag.feed(12000, 10);

        assertEquals(30, bag.getDeltaV(13000, 0));
        assertEquals(30, bag.getDeltaV(14000, 1));
        assertEquals(30, bag.getDeltaV(15000, 2));
    }


    private BucketAggregate makeAndFeedSl(int steps, int... stages) {
        BucketAggregate bag = new SlidingBucketAggregate(SEC, stages);

        for (int i = 1; i <= steps; i++) {
            bag.feed(i*SEC + SEC/2, 10);
        }

        return bag;
    }


    private void checkDeltas(BucketAggregate bag, long...deltas) {
        for (int i = 0; i < deltas.length; i++) {
            assertEquals(deltas[i], bag.getDeltaV(0L, i));
        }
    }

}
