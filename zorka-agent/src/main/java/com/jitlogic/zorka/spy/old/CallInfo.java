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
