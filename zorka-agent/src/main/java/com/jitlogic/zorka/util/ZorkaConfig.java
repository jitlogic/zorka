package com.jitlogic.zorka.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Zorka Configuration handling class. 
 * 
 * All configuration files, log files etc. are located in files 
 * relative to a directory pointed by zorka.home.dir property:  
 * 
 * ${zorka.home.dir}/conf  - config (.bsh) scripts;
 * ${zorka.home.dir}/log   - log files;
 * 
 * @author Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 */
public class ZorkaConfig {

	public static final String CONF_SUBDIR = "conf";
	public static final String LOG_SUBFIR = "log";
	
	private static Properties properties = null;
	private static String homeDir = null;
	
	
	public static String getLogDir() {
		return getHomeDir("log");
	}
	
	
	public static String getConfDir() {
		return getHomeDir("conf");
	}
	
	
	private synchronized static String getHomeDir(String suffix) {
		if (homeDir == null)
			homeDir = System.getProperty("zorka.home.dir", "/opt/zorka");			
		
		return homeDir.endsWith("/") ? homeDir + suffix : homeDir + "/" + suffix;
	}
	
	
	public static String get(String key) {
		return get(key, null);
	}
	
	
	
	
	public synchronized static String get(String key, String defVal) {
		
		if (properties == null) {
			loadProperties();
		}
		
		return properties.getProperty(key, defVal);
	}


	public static void loadProperties() {
		properties = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(getHomeDir("zorka.properties"));
			properties.load(is);
		} catch (IOException e) {
			
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) { }
		}
	}
	
	
	public static void put(String key, String val) {
		if (properties == null) {
			loadProperties();
		}
		
		properties.put(key, val);
	}
}
