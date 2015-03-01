/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * 
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.perfmon.RateAggregate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.jitlogic.zorka.core.test.support.ZorkaTestUtil;

public class SlidingWindowUnitTest extends ZorkaFixture {

	private ZorkaTestUtil testUtil;


	@Before
	public void setUp() {
        testUtil = ZorkaTestUtil.setUp();
	}


	@Test
	public void testQueryEmptyWindow() throws Exception {
		RateAggregate wnd = new RateAggregate(100, -1);
		assertEquals(-1.0, wnd.rate(), 0.0001);
	}
	
	
	@Test
	public void testSlidingWindowWithSingleItem() throws Exception {
		RateAggregate wnd = new RateAggregate(100, -1);
		wnd.feed(100, 1);
		assertEquals(0.0, wnd.rate(), 0.0001);
	}
	
	
	@Test
	public void testSlidingWindowWithTwoItems() throws Exception {
		RateAggregate wnd = new RateAggregate(100, -1);
		wnd.feed(100, 1);
		wnd.feed(150, 1);
		assertEquals(50.0, wnd.rate(), 0.0001);
	}

	
	@Test
	public void testWindowNotYetSlide() throws Exception {
		RateAggregate wnd = new RateAggregate(100, -1);
		testUtil.mockCurrentTimeMillis(0, 0, 50, 50, 100, 100, 100);
		wnd.feed(100);
		wnd.feed(150);
		wnd.feed(200);
		assertEquals(100.0, wnd.rate(), 0.0001);
	}

	@Test
	public void testWindowSlide() throws Exception {
		RateAggregate wnd = new RateAggregate(100, -1);
		testUtil.mockCurrentTimeMillis(0, 0, 50, 50, 100, 100, 150);
		wnd.feed(100);
		wnd.feed(150);
		wnd.feed(200);
		assertEquals(50.0, wnd.rate(), 0.0001);
	}
	
	@Test
	public void testWindowSlideOutOfHorizon() throws Exception {
		RateAggregate wnd = new RateAggregate(100, -1);
		testUtil.mockCurrentTimeMillis(0, 0, 50, 50, 100, 100, 1500);
		wnd.feed(100);
		wnd.feed(150);
		wnd.feed(200);
		assertEquals(-1.0, wnd.rate(), 0.0001);		
	}
}
