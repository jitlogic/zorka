package com.jitlogic.zorka.spy;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

import com.jitlogic.zorka.mbeans.MethodCallStatisticImpl;
import com.jitlogic.zorka.mbeans.MethodCallStats;
import com.jitlogic.zorka.util.ZorkaUtil;

public class MultiMethodDataCollector implements DataCollector {

	private MethodCallStats mcs;
	private long id = -1L;
	private String separator;
	int[] args;
	
	public MultiMethodDataCollector(MethodCallStats mcs, String separator, int[] args) {
		this.mcs = mcs;
		this.args = args;
		this.separator = separator;
		this.id = MainCollector.register(this);
	}
	
	
	public CallInfo logStart(long id, long tst, Object[] args) {
		return new CallInfo(id, tst, ZorkaUtil.join(separator, args));
	}
	
	
	public void logCall(long tst, CallInfo info) {
		String name = info.getTag();
		MethodCallStatisticImpl mci = mcs.getMethodCallStat(name);		
		mci.logCall(tst, System.nanoTime()-info.getTst());
	}
	
	
	public void logError(long tst, CallInfo info) {		
		String name = info.getTag();
		MethodCallStatisticImpl mci = mcs.getMethodCallStat(name);		
		mci.logError(tst, System.nanoTime()-info.getTst());
	}
	
	
	public MethodAdapter getAdapter(MethodVisitor mv) {
		return new SimpleMethodInstrumentator(mv, id, args);
	}
}
