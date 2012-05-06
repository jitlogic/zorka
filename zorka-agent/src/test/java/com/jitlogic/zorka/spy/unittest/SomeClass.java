package com.jitlogic.zorka.spy.unittest;

public class SomeClass {
	
	//public int i = 0;
	
	public long waitTime = 1L;
	
	public void someMethod() {
		TestUtil.sleep(waitTime);
		//i++;
	}
	
	public void errorMethod() throws TestException {
		TestUtil.sleep(waitTime);
		throw new TestException("bang!");
	}
	
	public void indirectErrorMethod() throws TestException {
		TestUtil.sleep(waitTime);
		errorMethod();
	}
	
	public void singleArgMethod(String arg1) {
		TestUtil.sleep(waitTime);
	}
	
	public void twoArgMethod(String arg1, String arg2) {
		TestUtil.sleep(waitTime);
	}

	public void threeArgMethod(String arg1, String arg2, String arg3) {
		TestUtil.sleep(waitTime);
	}
}
