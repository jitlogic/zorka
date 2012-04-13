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
