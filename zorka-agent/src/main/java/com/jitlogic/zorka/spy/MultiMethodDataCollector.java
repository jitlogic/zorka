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

package com.jitlogic.zorka.spy;

import java.util.List;

import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import org.objectweb.asm.MethodVisitor;

import com.jitlogic.zorka.mbeans.MethodCallStatistics;

public class MultiMethodDataCollector implements DataCollector {

	private MethodCallStatistics mcs;
	private long id = -1L;
	private SpyExpression expr;
	private int[] args;
	
	
	public MultiMethodDataCollector(MethodCallStatistics mcs, SpyExpression expr) {
		this.mcs = mcs;
		this.id = MainCollector.register(this);
		this.expr = expr;
		
		List<Integer> argMap = expr.getArgMap();
		args = new int[argMap.size()];
		
		for (int i = 0; i < args.length; i++)
			args[i] = argMap.get(i);
	}
	
	
	public CallInfo logStart(long id, long tst, Object[] vals) {
		return new CallInfo(id, tst, expr.format(vals));
	}
	
	
	public void logCall(long tst, CallInfo info) {
		String name = info.getTag();
		MethodCallStatistic mci = (MethodCallStatistic)mcs.getMethodCallStatistic(name);
		mci.logCall(tst, System.nanoTime()-info.getTst());
	}
	
	
	public void logError(long tst, CallInfo info) {		
		String name = info.getTag();
		MethodCallStatistic mci = (MethodCallStatistic)mcs.getMethodCallStatistic(name);
		mci.logError(tst, System.nanoTime()-info.getTst());
	}
	
	
	public MethodVisitor getAdapter(MethodVisitor mv) {
		return new SimpleMethodInstrumentator(mv, id, args);
	}
}
