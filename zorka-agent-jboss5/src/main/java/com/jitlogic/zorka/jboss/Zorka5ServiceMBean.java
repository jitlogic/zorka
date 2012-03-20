package com.jitlogic.zorka.jboss;

/* This file is part of ZORKA monitoring agent.
*
* ZORKA is free software: you can redistribute it and/or modify it under the
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

import org.jboss.system.ServiceMBean;

public interface Zorka5ServiceMBean extends ServiceMBean {
	
	public String zoolaQuery(String expr);
	
	public String zabbixQuery(String expr);
	
}
