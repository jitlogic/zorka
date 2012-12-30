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

/**
 * Represents all error conditions originating from inside Zorka code.
 */
public class ZorkaException extends Exception {

	private static final long serialVersionUID = 403339688211580095L;

    /**
     * Standard constructor.
     * @param msg message
     */
	public ZorkaException(String msg) {
		super(msg);
	}

    /**
     * Standard constructor
     *
     * @param msg message
     *
     * @param inner cause
     */
	public ZorkaException(String msg, Throwable inner) {
		super(msg, inner); 
	}
	
}
