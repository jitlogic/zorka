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


import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.rankproc.RankItem;
import com.jitlogic.zorka.agent.rankproc.RankList;
import com.jitlogic.zorka.agent.testutil.TestRankLister;


public class RankListerTest {

	TestRankLister lister;
	
	@Before
	public void setUp() {
		lister = new TestRankLister(60, 600);
	}
	
	@Test
	public void testCreateSimpleRankList() throws Exception {
		lister.testFeed(1,10,10, 2,20,20, 3,30,30, 4,40,40);
		lister.update(100);
		
		RankList<Long,Long[]> rlist = lister.newList("v1", "v1", 3);
		
		assertNotNull(rlist);
		assertNotNull(rlist.values());
	}
	
	
	@Test
	public void testCreateSimpleRankListAndRerank() throws Exception {
		lister.testFeed(1,10,10, 2,20,20, 3,30,30, 4,40,40);
		lister.update(10);
		
		RankList<Long,Long[]> rlist = lister.newList("v1", "v1", 3);		
		lister.rerank(10);
		
		Collection<?> vals = rlist.values();
		assertEquals(3, vals.size());
	}
	
	
	@Test
	public void testCreateDerivedAttr() throws Exception {
		lister.testFeed(1,10,10, 2,20,20, 3,30,30, 4,40,40);
		lister.newAttr("d1", "derived1", 60, 1, "v1", "tstamp");
		RankList<Long,Long[]> rlist = lister.newList("d1", "d1", 3);		

		lister.update(10);
		lister.rerank(10);
		
		Collection<?> vals = rlist.values();
		assertEquals(3, vals.size());
	}
	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testSimpleDeltaCalculation() throws Exception {
		lister.testFeed(1,10,10, 2,20,20, 3,30,30, 4,40,40);
		lister.testFeed(1,20,20, 2,22,22, 3,33,33, 4,44,44);
		lister.newAttr("d1", "derived1", 60, 1, "v1", "tstamp");
		RankList<Long,Long[]> rlist = lister.newList("d1", "d1", 3);		

		lister.update(10);
		lister.update(20);
		lister.rerank(20);
		
		Collection<?> vals = rlist.values();
		assertEquals(3, vals.size());
		assertEquals(1L, (long)(Long)((RankItem<Long,Long[]>)vals.toArray()[0]).getKey());
	}
	
	
	@Test
	public void testEmptyRankingBeforeRerank() throws Exception {
		RankList<Long,Long[]> rlist = lister.newList("v1", "v1", 3);		
		
		Collection<?> vals = rlist.values();
		assertEquals(0, vals.size());
	}
	
	// TODO test na okienkwoanie delty (3 próbki, 1 "wyjeżdża poza okienko)
	
	// TODO test na "garbage kolekcję" śledzonych obiektów (znikające obiekty) - usunięcie atrybutu "w trakcie" pracy;
	
	// TODO test na dodanie atrybutu "w trakcie" pracy;
	
	// TODO test na runCycle z jednym przebiegiem (rankingi z deltami powinny być nadal puste)
	
	// TODO test na runCycle z dwoma przebiegami (rankingi z deltami powinny się wypełnić)
}
