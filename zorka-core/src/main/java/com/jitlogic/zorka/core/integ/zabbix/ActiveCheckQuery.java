/**
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.integ.zabbix;

import java.util.ArrayList;


public class ActiveCheckQuery {

	private String request;
	private ArrayList<ActiveCheckResult> data;
	private long clock;

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public ArrayList<ActiveCheckResult> getData() {
		return data;
	}

	public void setData(ArrayList<ActiveCheckResult> results) {
		this.data = results;
	}
	
	public long getClock() {
		return clock;
	}
	
	public void setClock(long clock) {
		this.clock = clock;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder(100);
		stringBuilder.append("{request=");
		stringBuilder.append(request);
		stringBuilder.append(", data=[");
		
		for (ActiveCheckResult result : data){
			stringBuilder.append(result);
		}
		stringBuilder.append("], clock=");
		stringBuilder.append(clock);
		stringBuilder.append("}");
		
		return stringBuilder.toString();
	}
}
