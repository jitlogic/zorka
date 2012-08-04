package com.jitlogic.zorka.spy;

import org.objectweb.asm.MethodVisitor;

public interface DataCollector {
	public CallInfo logStart(long id, long tst, Object[] args);
	public void logCall(long tst, CallInfo info);
	public void logError(long tst, CallInfo info);
	public MethodVisitor getAdapter(MethodVisitor mv);
}
