package com.jitlogic.zorka.spy;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

import com.jitlogic.zorka.mbeans.MethodCallStatisticImpl;

public class SingleMethodDataCollector implements DataCollector {

	private long id = -1L;
	private MethodCallStatisticImpl mcs;
	
	public SingleMethodDataCollector(MethodCallStatisticImpl mcs) {
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

	public MethodAdapter getAdapter(MethodVisitor mv) {
		return new SimpleMethodInstrumentator(mv, id);
	}

	public long getId() {
		return id;
	}	
}
