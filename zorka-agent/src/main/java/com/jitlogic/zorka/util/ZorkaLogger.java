package com.jitlogic.zorka.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ZorkaLogger {

	private static ZorkaLogger logger = new ZorkaLogger("com.jitlogic.zorka");
	
	public static ZorkaLogger getLogger(Class<?> clazz) {
		return logger;
	}
	
	private Logger log;
	private boolean logExceptions = true; // TODO make this parameter configurable
	
	private ZorkaLogger(String name) {
		log = Logger.getLogger(name);
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
}
