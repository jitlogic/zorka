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

package com.jitlogic.zorka.agent.rateproc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AverageRateMetric {

	private Map<String,RateAggregate> windows;
	
	private final long defHorizon;
	private final long defValue;
	
	public AverageRateMetric(long defHorizon, long defValue) {
		this.defHorizon = defHorizon;
		this.defValue = defValue;
		windows = new ConcurrentHashMap<String, RateAggregate>();
	}
	
	private RateAggregate getWindow(String tag) {
		RateAggregate wnd = windows.get(tag);
		if (wnd == null) {
			wnd = new RateAggregate(defHorizon, defValue);
			
		}
		return wnd;
	}
	
	public void feed(String tag, long val) {
		getWindow(tag);
	}
	
}
