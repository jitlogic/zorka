package com.jitlogic.zorka.spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jitlogic.zorka.agent.JmxResolver;

public class SpyExpression {

	private final List<Integer> argMap;
	private final List<Object> segs;
	
	
	private SpyExpression(List<Integer> argMap, List<Object> segs) {
		this.argMap = Collections.unmodifiableList(argMap);
		this.segs = Collections.unmodifiableList(segs);
	}
	
	
	public List<Integer> getArgMap() {
		return argMap;
	}
	
	
	public List<Object> getSegs() {
		return segs;
	}
	
	
	public String format(Object[] args) {
		StringBuilder sb = new StringBuilder();
		
		for (Object seg : segs) {
			if (seg instanceof String) {
				sb.append(seg);
			} else {
				SpyExprMacro mac = (SpyExprMacro)seg;
				int idx = argMap.indexOf(mac.getArgnum());
				
				if (idx == -1 || idx >= args.length)
					throw new IllegalStateException("Cannot parse macro " + mac + 
						": argument number " + mac.getArgnum() + " is invalid." + 
						" This is propably an internal error.");
				
				Object val = args[idx];
				
				for (String attr : mac.getSegments())
					val = JmxResolver.get(val, attr);

				sb.append(val != null ? val.toString() : "null");
			}
		}
		
		return sb.toString();
	}
	
	
	public static SpyExpression parse(String expr) {
		
		// Split expression string into string/macro segments
		int pos = 0, bpos = 0;
		boolean inMacro = false;
		List<Object> segs = new ArrayList<Object>();
		
		while (pos < expr.length()) {
			if (inMacro) { // Inside a macro
				while (pos < expr.length() && expr.charAt(pos) != '}') { pos++; }
				segs.add(SpyExprMacro.parse(expr.substring(bpos, pos)));
				pos++; inMacro = false;
			} else { // Outside of macro
				while (pos < expr.length() && expr.charAt(pos) != '{') { pos++; }
				if (pos > bpos) segs.add(expr.substring(bpos, pos));
				pos++; inMacro = true;
			}
			bpos = pos;
		}

		// Generate argument map
		long argMask = 0L;
		List<Integer> argMap = new ArrayList<Integer>();
		
		for (Object obj : segs) {
			if (obj instanceof SpyExprMacro) {
				argMask |= (1 << ((SpyExprMacro)obj).getArgnum());
			}
		}
		
		for (int i = 0; i < 31; i++) {
			if (0 != (argMask & (1<<i))) {
				argMap.add(i);
			}
		}
		
		return new SpyExpression(argMap, segs);
	}
	
}
