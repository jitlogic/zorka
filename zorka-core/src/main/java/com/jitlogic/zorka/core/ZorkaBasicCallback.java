/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core;

/**
 * This is default implementation of ZorkaCallback interface.
 */
public class ZorkaBasicCallback implements ZorkaCallback {

    /** Stored result */
	private Object result;

    /** Stored error */
	private Throwable error;

    @Override
	public void handleResult(Object result) {
		this.result = result;
	}

    @Override
	public void handleError(Throwable e) {
		this.error = e;
	}

    /** Returns result */
	public Object getResult() {
		return result;
	}

    /** Returns error */
	public Throwable getError() {
		return error;
	}
}
