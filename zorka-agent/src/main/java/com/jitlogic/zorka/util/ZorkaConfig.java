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

import com.jitlogic.zorka.bootstrap.AgentMain;

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

    public synchronized void setHomeDir(String dir) {
        homeDir = dir;
    }
	
	private synchronized static String getHomeDir(String suffix) {
		if (homeDir == null)
            homeDir = AgentMain.getHomeDir() != null ? AgentMain.getHomeDir() : "/opt/zorka";
			//homeDir = System.getProperty("zorka.home.dir", "/opt/zorka");
		
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
