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

import com.jitlogic.zorka.agent.AgentMain;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

	private static Properties properties = null;
	private static String homeDir = null;

    public final static String DEFAULT_CONFDIR = "/opt/zorka";


    /**
     * Clears static agent configuration. This is mainly useful for tests,
     * but it might be also useful for online agent reload feature (which
     * might be implemented some time in the future).
     */
    public static void cleanup() {
        properties = null;
        homeDir = null;
    }


    public static Properties getProperties() {
        return properties;
    }

    /**
     * Set configuration properties manually. This is useful mainly for testing.
     *
     * @param props new properties
     */
    public static void setProperties(Properties props) {
        homeDir = props.getProperty("zorka.home.dir", "/opt/zorka");
        properties = props;
    }


    /**
     * @return directory where agent will write its logs.
     */
    public static String getLogDir() {
		return getHomeDir("log");
	}


    /**
     * @return directory from which agent reads BSH configuration scripts.
     */
	public static String getConfDir() {
		return getHomeDir("conf");
	}


	private synchronized static String getHomeDir(String suffix) {
		return homeDir.endsWith("/") ? homeDir + suffix : homeDir + "/" + suffix;
	}


    /**
     * Sets agent home directory and load zorka.properties file from it.
     *
     * @param home home directory for zorka agent
     *
     */
	public static void loadProperties(String home) {
        homeDir = home;
		properties = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(getHomeDir("zorka.properties"));
			properties.load(is);
		} catch (IOException e) {
			// TODO what to do here ?
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) { }
		}

        properties.put("zorka.home.dir", homeDir);
        properties.put("zorka.config.dir", getHomeDir("conf"));
	}
	

}
