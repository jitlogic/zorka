/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.perfmon.BucketAggregate;

import com.jitlogic.zorka.core.perfmon.CircularBucketAggregate;
import org.junit.Test;
import static org.junit.Assert.*;

public class AggregateCountingUnitTest {

    @Test
    public void testCircularV1() {
        BucketAggregate bag = new CircularBucketAggregate(1, 10, 60, 300, 900);

        bag.feed(10, 10);
        bag.feed(11, 10);
        bag.feed(12, 10);

        assertEquals(30, bag.getDeltaV(0, 13));
        assertEquals(30, bag.getDeltaV(1, 14));
        assertEquals(30, bag.getDeltaV(2, 15));
    }


    @Test
    public void testCircularV2() {
        BucketAggregate bag = new CircularBucketAggregate(1, 4, 60);

        bag.feed(10, 10);
        bag.feed(50, 10);
        bag.feed(90, 10);

        assertEquals(20, bag.getDeltaV(0, 91));
    }


    @Test
    public void testCircularTimeInOneSection() {
        BucketAggregate bag = new CircularBucketAggregate(1, 4, 60);

        bag.feed(4, 1);
        bag.feed(11, 1);

        assertEquals(7, bag.getDeltaT(0, 12));
    }


    @Test
    public void testCircularTimeAcrossManySections() {
        BucketAggregate bag = new CircularBucketAggregate(1, 4, 60);

        bag.feed(5, 1);
        bag.feed(45, 1);

        assertEquals(40, bag.getDeltaT(0, 46));
    }

    @Test
    public void testFeedSomeDataAndAskWayLater() {
        BucketAggregate bag = new CircularBucketAggregate(1, 10, 60, 300, 900);

        bag.feed(5, 1);
    }
}
