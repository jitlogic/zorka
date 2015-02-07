/**
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


public class ActiveCheckResponse {

	private String response;
	private ArrayList<ActiveCheckQueryItem> data;

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public ArrayList<ActiveCheckQueryItem> getData() {
		return data;
	}

	public void setData(ArrayList<ActiveCheckQueryItem> data) {
		this.data = data;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder(100);
		stringBuilder.append("{response=");
		stringBuilder.append(response);
		stringBuilder.append(", data=[");
		
		int size = data.size();
		if (size > 0) {
			stringBuilder.append(data.get(0));
			for (int i = 1; i < size; i++) {
				stringBuilder.append(", ");
				stringBuilder.append(data.get(i));
			}
		}
		stringBuilder.append("]}");
		
		return stringBuilder.toString();
	}
}
