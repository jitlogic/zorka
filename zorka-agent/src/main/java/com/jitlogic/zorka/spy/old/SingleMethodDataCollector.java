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

package com.jitlogic.zorka.spy.old;

import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import org.objectweb.asm.MethodVisitor;

public class SingleMethodDataCollector implements DataCollector {

	private long id = -1L;
	private MethodCallStatistic mcs;
	
	public SingleMethodDataCollector(MethodCallStatistic mcs) {
		this.mcs = mcs;
		this.id = MainCollector.register(this);
	}
	
	public CallInfo logStart(long id, long tst, Object[] args) {
		return new CallInfo(id, tst, null);
	}

	public void logCall(long tst, CallInfo info) {
		// TODO wyrugować System.nanoTime() ? 
		mcs.logCall(tst, System.nanoTime()-info.getTst());
	}

	public void logError(long tst, CallInfo info) {
		// TODO wyrugować System.nanoTime() ? 
		mcs.logError(tst, System.nanoTime()-info.getTst());
	}

	public MethodVisitor getAdapter(MethodVisitor mv) {
		return new SimpleMethodInstrumentator(mv, id);
	}

	public long getId() {
		return id;
	}	
}
