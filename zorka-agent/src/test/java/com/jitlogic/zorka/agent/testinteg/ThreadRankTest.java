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

package com.jitlogic.zorka.agent.testinteg;

import java.lang.management.ThreadInfo;
import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.ZorkaUtil;
import com.jitlogic.zorka.agent.rankproc.RankList;
import com.jitlogic.zorka.agent.rankproc.ThreadRankLister;

public class ThreadRankTest {
	
	@Test
	public void testCreateSimpleRankList() throws Exception {
		ThreadRankLister lister = new ThreadRankLister(60000, 600000);
		lister.update(ZorkaUtil.currentTimeMillis());
		
		RankList<Long,ThreadInfo> rlist = lister.newList("cpu1", "cpuTime", 3);		
		assertNotNull("Should return some list", rlist);
		
		Collection<?> vals = rlist.values();
		assertNotNull(vals);
	}
	
	
	@Test
	public void testCreateSimpleRankListAndRerank() throws Exception {
		ThreadRankLister lister = new ThreadRankLister(60000, 600000);
		lister.update(ZorkaUtil.currentTimeMillis());
		
		RankList<Long,ThreadInfo> rlist = lister.newList("cpu1", "cpuTime", 3);		
		lister.rerank(ZorkaUtil.currentTimeMillis());
		
		Collection<?> vals = rlist.values();
		assertEquals(3, vals.size());
	}
	
	
	@Test
	public void testCreateDerivedAttr() throws Exception {
		ThreadRankLister lister = new ThreadRankLister(60000, 600000);
		lister.newAttr("cpu1", "Cpu utilization avg 1m", 60000, 1, "cpuTime", "tstamp");
		RankList<Long,ThreadInfo> rlist = lister.newList("cpu1", "cpu1", 3);		

		lister.update(ZorkaUtil.currentTimeMillis());
		lister.rerank(ZorkaUtil.currentTimeMillis());
		
		Collection<?> vals = rlist.values();
		assertEquals(3, vals.size());
	}
}

