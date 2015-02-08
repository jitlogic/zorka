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

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.List;

public class ActiveCheckResponse {

	private String response;
	private List<ActiveCheckQueryItem> data;

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public List<ActiveCheckQueryItem> getData() {
		return data;
	}

	public void setData(List<ActiveCheckQueryItem> data) {
		this.data = data;
	}

    @Override
    public int hashCode() {
        return 31 * (response != null ? response.hashCode() : 1) +
            17 * (data != null ? data.hashCode() : 1);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ActiveCheckResponse) {
            ActiveCheckResponse r = (ActiveCheckResponse)obj;
            return ZorkaUtil.objEquals(response, r.response) && ZorkaUtil.objEquals(data, r.data);
        }
        return false;
    }

	@Override
	public String toString(){
        return "{response=" + response + ", data=" + data + "}";
	}


}
