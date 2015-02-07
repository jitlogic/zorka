/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.common.util.ZorkaUtil;

/**
 * Test double for ZorkaUtil class.
 * 
 * @author Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 */
public class ZorkaTestUtil extends ZorkaUtil {

	private long[] tsValues = null;
	private int tsIndex = -1;
	
	public void mockCurrentTimeMillis(final long...tstamps) {
		tsValues = tstamps.length > 0 ? tstamps : null;
		tsIndex = -1;
	}
	
	public static ZorkaTestUtil setUp() {
		ZorkaTestUtil instance = new ZorkaTestUtil();
        instanceRef.set(instance);
		return (ZorkaTestUtil)instance;
	}
	
	public static void tearDown() {
		instanceRef.set(null);
	}
	
	public long currentTimeMillis() {
		if (tsValues != null) {
			if (tsIndex < tsValues.length-1) {
				tsIndex++;
			}
			return tsValues[tsIndex];
		} else {
			return System.currentTimeMillis();
		}
	}
}
