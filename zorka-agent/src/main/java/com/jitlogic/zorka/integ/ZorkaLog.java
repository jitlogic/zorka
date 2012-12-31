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

package com.jitlogic.zorka.integ;


public class ZorkaLog {

    private String tag;
    private ZorkaLogger output;


    ZorkaLog(String tag, ZorkaLogger output) {
        this.tag = tag;
        this.output = output;
    }

	public void trace(String msg, Object...args) {
		output.log(ZorkaLogLevel.TRACE, tag, msg, null, args);
	}


	public void debug(String msg, Object...args) {
        output.log( ZorkaLogLevel.DEBUG, tag, msg, null, args);
	}


	public void info(String msg, Object...args) {
        output.log(ZorkaLogLevel.INFO, tag, msg, null, args);
	}


	public void warn(String msg, Object...args) {
        output.log(ZorkaLogLevel.WARN, tag, msg, null, args);
	}


	public void error(String msg, Object...args) {
        output.log(ZorkaLogLevel.ERROR, tag, msg, null, args);
	}
	
	
	public void error(String msg, Throwable e, Object args) {
        output.log(ZorkaLogLevel.ERROR, tag, msg, e, args);
	}

}
