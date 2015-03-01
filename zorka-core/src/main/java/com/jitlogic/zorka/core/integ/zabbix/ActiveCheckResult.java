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

public class ActiveCheckResult {

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

