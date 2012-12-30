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
 * Callback for asynchronously called ZORKA queries.
 * 
 * @author rafal.lewczuk@jitlogic.com
 *
 */
public interface ZorkaCallback {

    /**
     * This method is called after successful execution.
     *
     * @param rslt execution result
     */
	public void handleResult(Object rslt);

    /**
     * This method is called when error occurs.
     *
     * @param e exception thrown while executing query
     */
	public void handleError(Throwable e);
	
}
