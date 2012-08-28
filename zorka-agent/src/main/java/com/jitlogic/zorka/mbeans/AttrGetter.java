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

package com.jitlogic.zorka.mbeans;

import com.jitlogic.zorka.util.ObjectInspector;

public class AttrGetter implements ValGetter {

	private Object obj;
	private String[] attrs;

    private ObjectInspector inspector = new ObjectInspector();
	
	public AttrGetter(Object obj, String...attrs) {
		this.obj = obj;
		this.attrs = attrs;
	}
	
	public Object get() {
		Object v = obj;
		
		for (String attr : attrs)
			v = inspector.get(v, attr);
		
		return v;
	}

}
