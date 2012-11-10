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

package com.jitlogic.zorka.rankproc;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * 
 * Standard attributes:
 * <li>id (Long) - thread ID;
 * <li>timestamp - timestamp;
 * <li>name (String) - thread name;
 * <li>state (State) - thread state;
 * <li>cpuTime (long) - CPU time;
 * <li>cpuUtil (double) - CPU utilization;
 * <li>blockedTime (long)   - blocked time;
 * <li>blockedUtil (double) - percent of time thread has spent in blocked state; 
 * <li>blockedCount (long)  - number of times thread has been blocked;
 * <li>userTime (long) - user time 

 * @author RLE <rafal.lewczuk@jitlogic.com>
 *
 */
public class OldThreadRankLister extends OldRankLister<Long,ThreadInfo> {

	private final static String[] xbasicAttr = { 
		"id", "tstamp", "name", "state", 
		"cpuTime", "blockedTime", "blockedCount", "userTime" };
	
	private final static String[] xbasicDesc = { 
		"Thread ID", "Timestamp", "Thread Name", "Thread State", 
		"CPU Time", "Blocked Time", "Blocked Count", "User Time" };
	
	private final static OpenType[] xbasicType = { 
		SimpleType.LONG, SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, 
		SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG };
	
	private static final int ATTR_NAME          = 2;
	private static final int ATTR_STATE         = 3;
	private static final int ATTR_CPU_TIME      = 4;
	private static final int ATTR_BLOCKED_TIME  = 5;
	private static final int ATTR_BLOCKED_COUNT = 6;
	private static final int ATTR_USER_TIME     = 7;
	
	private ThreadMXBean threadMXBean;
	
	public OldThreadRankLister(long updateInterval, long rerankInterval) {
		super(updateInterval, rerankInterval, xbasicAttr, xbasicDesc, xbasicType);
		threadMXBean = ManagementFactory.getThreadMXBean();
		makeCompositeType();
	}
		
	
	public void updateBasicAttrs(OldRankItem<Long,ThreadInfo> item, ThreadInfo info, long tstamp) {
		Object[] v = item.getValues();
		
		v[ATTR_ID] = info.getThreadId();
		v[ATTR_NAME] = info.getThreadName();
		v[ATTR_TSTAMP] = tstamp;
		v[ATTR_STATE] = info.getThreadState().toString();
		v[ATTR_CPU_TIME] = threadMXBean.getThreadCpuTime(info.getThreadId());
		v[ATTR_BLOCKED_TIME] = info.getBlockedTime();
		v[ATTR_BLOCKED_COUNT] = info.getBlockedCount();
		v[ATTR_USER_TIME] = threadMXBean.getThreadUserTime(info.getThreadId());
	}

	@Override
	public Long getKey(ThreadInfo info) {
		return info.getThreadId();
	}

	@Override
	public List<ThreadInfo> list() {
		long[] ids = threadMXBean.getAllThreadIds();
		List<ThreadInfo> tlist = new ArrayList<ThreadInfo>(ids.length+2);
		for (long id : ids) {
			ThreadInfo ti = threadMXBean.getThreadInfo(id);
			if (ti != null) tlist.add(ti);
		}
		return tlist;
	}


}
