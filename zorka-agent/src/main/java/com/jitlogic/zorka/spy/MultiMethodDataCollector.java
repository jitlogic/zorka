package com.jitlogic.zorka.spy;

import java.util.List;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

import com.jitlogic.zorka.mbeans.MethodCallStatisticImpl;
import com.jitlogic.zorka.mbeans.MethodCallStats;

public class MultiMethodDataCollector implements DataCollector {

	private MethodCallStats mcs;
	private long id = -1L;
	private SpyExpression expr;
	private int[] args;
	
	
	public MultiMethodDataCollector(MethodCallStats mcs, SpyExpression expr) {
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
