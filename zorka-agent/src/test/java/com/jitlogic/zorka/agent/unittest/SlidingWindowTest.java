/** 
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.rankproc.OldRateAggregate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.testutil.ZorkaTestUtil;

public class SlidingWindowTest extends ZorkaFixture {

	private ZorkaTestUtil testUtil;


	@Before
	public void setUp() {
        testUtil = ZorkaTestUtil.setUp();
	}


	@Test
	public void testQueryEmptyWindow() throws Exception {
		OldRateAggregate wnd = new OldRateAggregate(100, -1);
		assertEquals(-1.0, wnd.rate(), 0.0001);
	}
	
	
	@Test
	public void testSlidingWindowWithSingleItem() throws Exception {
		OldRateAggregate wnd = new OldRateAggregate(100, -1);
		wnd.feed(100, 1);
		assertEquals(0.0, wnd.rate(), 0.0001);
	}
	
	
	@Test
	public void testSlidingWindowWithTwoItems() throws Exception {
		OldRateAggregate wnd = new OldRateAggregate(100, -1);
		wnd.feed(100, 1);
		wnd.feed(150, 1);
		assertEquals(50.0, wnd.rate(), 0.0001);
	}

	
	@Test
	public void testWindowNotYetSlide() throws Exception {
		OldRateAggregate wnd = new OldRateAggregate(100, -1);
		testUtil.mockCurrentTimeMillis(0, 0, 50, 50, 100, 100, 100);
		wnd.feed(100);
		wnd.feed(150);
		wnd.feed(200);
		assertEquals(100.0, wnd.rate(), 0.0001);
	}

	@Test
	public void testWindowSlide() throws Exception {
		OldRateAggregate wnd = new OldRateAggregate(100, -1);
		testUtil.mockCurrentTimeMillis(0, 0, 50, 50, 100, 100, 150);
		wnd.feed(100);
		wnd.feed(150);
		wnd.feed(200);
		assertEquals(50.0, wnd.rate(), 0.0001);
	}
	
	@Test
	public void testWindowSlideOutOfHorizon() throws Exception {
		OldRateAggregate wnd = new OldRateAggregate(100, -1);
		testUtil.mockCurrentTimeMillis(0, 0, 50, 50, 100, 100, 1500);
		wnd.feed(100);
		wnd.feed(150);
		wnd.feed(200);
		assertEquals(-1.0, wnd.rate(), 0.0001);		
	}
}
