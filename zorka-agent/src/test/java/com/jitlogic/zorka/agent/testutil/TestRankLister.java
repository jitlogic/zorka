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

package com.jitlogic.zorka.agent.testutil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.jitlogic.zorka.rankproc.RankItem;
import com.jitlogic.zorka.rankproc.RankLister;

/**
 * Test double for rankproc unit tests.
 * 
 * @author RLE <rle@jitlogic.com>
 *
 */
public class TestRankLister extends RankLister<Long, Long[]> {

	private Queue<List<Long[]>> queue = new LinkedList<List<Long[]>>();
	
	public TestRankLister(long updateInterval, long rerankInterval) {
		super(updateInterval, rerankInterval, 
			new String[] { "id", "tstamp", "v1", "v2" },
			new String[] { "ID", "Timestamp", "Value1", "Value2" }, 
			new OpenType[] { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,  });
	}

	
	/**
	 * Adds one "shot" of test data. Data are passed as a sequence of integers
	 * that are automatically split into triples (id,v1,v2)
	 * 
	 * @param tuples input data (all tuples flattened into a single sequence)
	 */
	public void testFeed(int...tuples) {
		List<Long> tuple = new ArrayList<Long>(3);
		List<Long[]> data = new ArrayList<Long[]>();
		
		for (int v : tuples) {
			tuple.add((long)v);
			if (tuple.size() == 3) {
				data.add(tuple.toArray(new Long[0]));
				tuple.clear();
			}
		}
		queue.add(data);
	}
	
	
	@Override
	public void updateBasicAttrs(RankItem<Long, Long[]> item, Long[] info, long tstamp) {
		Object[] v = item.getValues();
		v[ATTR_ID] = info[0];
		v[ATTR_TSTAMP] = tstamp;
		v[2] = info[1]; v[3] = info[2];
	}
	
	
	@Override
	public Long getKey(Long[] info) {
		return info[0];
	}
	
	
	@Override
	public List<Long[]> list() {
		return queue.remove();
	}
}
