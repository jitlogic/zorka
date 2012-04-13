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

package com.jitlogic.zorka.agent;

public class ZorkaBasicCallback implements ZorkaCallback {

	private Object result = null;
	private Throwable error = null;
	
	public void handleResult(Object result) {
		this.result = result;
	}

	public void handleError(Throwable e) {
		this.error = e;
	}
	
	@Override
	public String toString() {
		return error != null ? 
				ObjectDumper.errorDump(error) : 
				ObjectDumper.objectDump(result);
	}
	
	public Object getResult() {
		return result;
	}
	
	public Throwable getError() {
		return error;
	}
}
