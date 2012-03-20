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

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class JmxResolver {
	
	@SuppressWarnings("unchecked")
	public Set<ObjectName> queryNames(MBeanServerConnection conn, String query) throws ZorkaError {
		try {
			ObjectName on = new ObjectName(query);
			return (Set<ObjectName>)conn.queryNames(on, null);
		} catch (Exception e) {
			throw new ZorkaError("Error resolving object names.", e);
		}
	}
	
}
