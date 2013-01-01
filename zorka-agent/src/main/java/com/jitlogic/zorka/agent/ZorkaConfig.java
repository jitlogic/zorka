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

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.io.File;
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
 * @author rafal.lewczuk@jitlogic.com
 *
 */
public class ZorkaConfig {

    /** Logger */
    private final static ZorkaLog log = ZorkaLogger.getLog(ZorkaConfig.class);

    /** Configuration properties */
    private static Properties properties = null;

    /** Home directory */
	private static String homeDir = null;

    /** Path to config defaults (always in classpath) */
    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/agent/zorka.properties";

    /**
     * Clears static agent configuration. This is mainly useful for tests,
     * but it might be also useful for online agent reload feature (which
     * might be implemented some time in the future).
     */
    public static void cleanup() {
        properties = null;
        homeDir = null;
    }


    /**
     * Returns configuration properties.
     *
     * @return configuration properties
     */
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
     * Returns path to log directory.
     *
     * @return directory where agent will write its logs.
     */
    public static String getLogDir() {
		return getHomeDir("log");
	}


    /**
     * Returns path to configuration directory.
     *
     * @return directory from which agent reads BSH configuration scripts.
     */
	public static String getConfDir() {
		return getHomeDir("conf");
	}


    /**
     * Returns home directory plus suffix.
     * @param suffix
     * @return
     */
	private synchronized static String getHomeDir(String suffix) {
		return homeDir.endsWith(File.separator) ? homeDir + suffix : homeDir + File.separator + suffix;
	}


    /**
     * Returns default configuration properties. This is read from property file embedded
     * in agent jar file.
     *
     * @return default configuration properties.
     */
    public static Properties defaultProperties() {
        Properties props = new Properties();

        InputStream is = null;

        try {
            is = ZorkaConfig.class.getResourceAsStream(DEFAULT_CONF_PATH);
            props.load(is);
            is.close();
        } catch (IOException e) {
            log.error("Error loading default properties", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("Error closing default properties file", e);
                }
                is = null;
            }
        }

        return props;
    }

    /**
     * Sets agent home directory and load zorka.properties file from it.
     *
     * @param home home directory for zorka agent
     */
	public static void loadProperties(String home) {
        homeDir = home;
		properties = defaultProperties();
		InputStream is = null;
		try {
			is = new FileInputStream(getHomeDir("zorka.properties"));
			properties.load(is);
		} catch (IOException e) {
			log.error("Error loading zorka.properties");
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
                    log.error("Error closing zorka.properties file", e);
                }
		}

        properties.put("zorka.home.dir", homeDir);
        properties.put("zorka.config.dir", getHomeDir("conf"));
        properties.put("zorka.log.dir", getHomeDir("log"));
	}
	

}
