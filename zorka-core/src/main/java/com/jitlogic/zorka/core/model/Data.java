package com.jitlogic.zorka.core.model;

public class Data {

	private String host;
	protected String key;
	private String value;
	protected int lastlogsize;
	private long clock;
	
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public int getLastlogsize() {
		return lastlogsize;
	}
	
	public void setLastlogsize(int lastlogsize) {
		this.lastlogsize = lastlogsize;
	}

	public long getClock() {
		return clock;
	}

	public void setClock(long clock) {
		this.clock = clock;
	}

	@Override
	public String toString() {
		return "{host=" + host + ", key=" + key + ", value=" + value + ", lastlogsize=" + lastlogsize + ", clock=" + clock + "}";
	}
}

