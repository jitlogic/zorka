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

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class ZorkaLogger {

	private static ZorkaLogger logger = null;
	
	public synchronized static ZorkaLogger getLogger(Class<?> clazz) {
		if (logger == null) {
			logger = new ZorkaLogger("com.jitlogic.zorka");
		}
		return logger;
	}
	
	private Logger log;
	private boolean logExceptions;
	private boolean doTrace;
	
	private ZorkaLogger(String name) {
		logExceptions = "yes".equalsIgnoreCase(ZorkaConfig.get("zorka.log.exceptions", "yes"));
		doTrace = "yes".equalsIgnoreCase(ZorkaConfig.get("zorka.log.trace", "no"));
		log = Logger.getLogger(name);
		log.setLevel(Level.parse(ZorkaConfig.get("zorka.log.level", "ALL")));
		try {
			FileHandler handler = new FileHandler(
				ZorkaConfig.get("zorka.log.file", 
					ZorkaConfig.getLogDir() + "/zorka_%u.log"), 
					1024*1024, 10);
			handler.setLevel(Level.ALL);
			handler.setFormatter(new SimpleFormatter());
			log.addHandler(handler);
		} catch (IOException e) { }
	}
	
	public void trace(String msg) {
		log.finest(msg);
	}
	
	public void debug(String msg) {
		log.fine(msg);
	}
	
	public void notice(String msg) {
		log.log(Level.CONFIG, msg);
	}
	
	public void info(String msg) {
		log.info(msg);
	}
	
	public void warn(String msg) {
		log.log(Level.WARNING, msg);
	}
	
	public void error(String msg) {
		log.log(Level.WARNING, msg);
	}
	
	
	public void error(String msg, Throwable e) {
		if (logExceptions) {
			log.log(Level.WARNING, msg, e);
		} else {
			log.log(Level.WARNING, msg);
		}
	}
	
	public boolean isTrace() {
		return doTrace;
	}
}
