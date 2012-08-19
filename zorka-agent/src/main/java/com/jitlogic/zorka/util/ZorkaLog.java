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

package com.jitlogic.zorka.util;


public class ZorkaLog {

    private Class<?> clazz;
    private ZorkaLogger output;


	ZorkaLog(Class<?> clazz, ZorkaLogger output) {
        this.clazz = clazz;
        this.output = output;
	}


	public void trace(String msg) {
		output.log(this, ZorkaLogLevel.TRACE, msg);
	}


	public void debug(String msg) {
        output.log(this, ZorkaLogLevel.DEBUG, msg);
	}


	public void info(String msg) {
        output.log(this, ZorkaLogLevel.INFO, msg);
	}


	public void warn(String msg) {
        output.log(this, ZorkaLogLevel.WARN, msg);
	}


	public void error(String msg) {
        output.log(this, ZorkaLogLevel.ERROR, msg);
	}
	
	
	public void error(String msg, Throwable e) {
        output.log(this, ZorkaLogLevel.ERROR, msg, e);
	}

    public Class<?> getClazz() {
        return clazz;
    }
}
