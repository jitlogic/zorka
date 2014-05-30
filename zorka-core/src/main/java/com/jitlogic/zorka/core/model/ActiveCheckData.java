package com.jitlogic.zorka.core.model;

public class ActiveCheckData {
	
	protected String key;
	private int delay;
	protected int lastlogsize;
	private int mtime;
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public int getDelay() {
		return delay;
	}
	
	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	public int getLastlogsize() {
		return lastlogsize;
	}
	
	public void setLastlogsize(int lastlogsize) {
		this.lastlogsize = lastlogsize;
	}
	
	public int getMtime() {
		return mtime;
	}
	
	public void setMtime(int mtime) {
		this.mtime = mtime;
	}
	
	@Override
	public String toString() {
		return "{key=" + key + ", delay=" + delay + ", lastlogsize=" + lastlogsize + ", mtime=" + mtime + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + delay;
//		result = prime * result + lastlogsize;
		result = prime * result + mtime;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) 
			return true;
		
		if (obj == null) 
			return false;
		
		if (getClass() != obj.getClass()) 
			return false;
		
		ActiveCheckData other = (ActiveCheckData) obj;
		
		if (key == null)
			if (other.key != null) 
				return false;
		else 
			if (!key.equals(other.key)) 
				return false;
		
		if (delay != other.delay) 
			return false;
		
//		if (lastlogsize != other.lastlogsize) 
//			return false;
		
		if (mtime != other.mtime) 
			return false;
		
		return true;
	}
	
	
}
