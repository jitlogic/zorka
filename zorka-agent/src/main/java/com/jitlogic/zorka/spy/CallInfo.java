package com.jitlogic.zorka.spy;

public class CallInfo {
	private long id;
	private long tst;
	private String tag;
	
	public CallInfo(long id, long tst, String tag) {
		this.id = id;
		this.tst = tst;
		this.tag = tag;
	}
	
	public long getId() {
		return id;
	}
	
	public long getTst() {
		return tst;
	}
	
	public String getTag() {
		return tag;
	}
	
}
