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

public class MethodTemplate {

	private String className, methodName, signature;
	private boolean classRex, methodRex;
	
	private DataCollector collector;
	
	public MethodTemplate(String className, String methodName, String  signature, DataCollector collector) {
		
		this.classRex = className.startsWith("~");
		this.className = classRex ? className.substring(1) : className;
		
		this.methodRex = methodName.startsWith("~");
		this.methodName = methodRex ? methodName.substring(1) : methodName;
				
		this.signature = signature;
		
		this.collector = collector;
	}
	
	
	public boolean match(String className) {
		return classRex ? className.matches(this.className) : className.equals(this.className);
	}
	
	
	public boolean match(String className, String methodName, String signature) {
		return (classRex ? className.matches(this.className) : className.equals(this.className)) &&
				(methodRex ? methodName.matches(this.methodName) : methodName.equals(this.methodName)) &&
				(this.signature == null || this.signature.equals(signature));
	}
	
	public DataCollector mkCollector(String className, String methodName, String signature) {
		return collector;
	}
}
