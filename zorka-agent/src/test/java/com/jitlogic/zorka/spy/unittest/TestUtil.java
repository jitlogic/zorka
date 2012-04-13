package com.jitlogic.zorka.spy.unittest;

import java.lang.reflect.Method;

import org.objectweb.asm.ClassReader;

import com.jitlogic.zorka.spy.ZorkaSpy;

public class TestUtil extends ClassLoader {

	public static Object instrumentAndInstantiate(ZorkaSpy spy, String className) throws Exception {
		ClassReader cr = new ClassReader(className);
		byte[] buf = spy.instrument(className, null, cr);
		if (buf == null) return null;
		Class<?> clazz = new TestUtil().defineClass(className, buf, 0, buf.length);
		return clazz.newInstance();
	}

	public static void callMethod(Object obj, String name) throws Exception {
		Method method = null;
		Class<?> clazz = obj.getClass();
		
		for (Method met : clazz.getMethods()) {
			if (name.equals(met.getName())) {
				method = met;
				break;
			}
		}
		
		if (method != null)
			method.invoke(obj);
	}
	
	public static void sleep(long waitTime) {
		if (waitTime <= 0L) return;
		
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
