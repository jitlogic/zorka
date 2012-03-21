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

import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxResolver {
	
	private static final Logger log = LoggerFactory.getLogger(JmxResolver.class); 
	
	public Set<ObjectName> queryNames(MBeanServerConnection conn, String query) {
		try {
			ObjectName on = new ObjectName(query);
			return (Set<ObjectName>)conn.queryNames(on, null);
		} catch (Exception e) {
			ZorkaUtil.error(log, "Error resolving object names.", e);
			return new HashSet<ObjectName>();
		}
	}
	
}
