/**
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.integ.zabbix;

/**
 * Represents single item from active check query.
 */
public class ActiveCheckQueryItem {

    /**
     * Item key.
     */
	protected String key;

    /**
     * Item iterval (seconds).
     */
	private int delay;


    /**
     * Last position (if applicable).
     */
	protected int lastlogsize;


    /**
     * Last item modification time.
     */
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
		
		ActiveCheckQueryItem other = (ActiveCheckQueryItem) obj;
		
		if (key == null)
			if (other.key != null) 
				return false;
		else 
			if (!key.equals(other.key)) 
				return false;
		
		if (delay != other.delay) 
			return false;
		
		if (mtime != other.mtime)
			return false;
		
		return true;
	}


}
