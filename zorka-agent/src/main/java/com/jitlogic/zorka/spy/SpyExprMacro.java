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

package com.jitlogic.zorka.spy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpyExprMacro {
	
	
	private final int num;
	private final List<String> segs;
	
	
	public SpyExprMacro(int num, List<String> segs) {
		this.num = num;
		this.segs = Collections.unmodifiableList(segs);
	}
	
	
	public static SpyExprMacro parse(String src) {
		String[] segs = src.split("\\.");
		if (!segs[0].matches("^[0-9]+$")) { return null; }
		
		int num = Integer.parseInt(segs[0]);
		if (num > 31) { return null; }
		
		List<String> segments = Arrays.asList(segs);
		
		return new SpyExprMacro(num, segments.subList(1, segments.size()));
	}
	
	
	public int getArgnum() {
		return num;
	}
	
	
	public List<String> getSegments() {
		return segs;
	}
	
	
	@Override
	public int hashCode() {
		return 31 * num;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof SpyExprMacro &&
			((SpyExprMacro)obj).num == num &&
			((SpyExprMacro)obj).segs.equals(segs);
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{arg"); sb.append(num);
		
		for (String seg : segs) {
			sb.append("."); sb.append(seg); 
		}
		
		sb.append('}');
		
		return sb.toString();
	}
}
