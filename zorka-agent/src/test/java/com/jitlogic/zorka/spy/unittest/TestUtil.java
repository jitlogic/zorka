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

	public static void callMethod(Object obj, String name, Object...args) throws Exception {
		Method method = null;
		Class<?> clazz = obj.getClass();
		
		for (Method met : clazz.getMethods()) {
			if (name.equals(met.getName())) {
				method = met;
				break;
			}
		}
		
		if (method != null) {
			if (args.length == 0) {
				method.invoke(obj);
			} else {
				method.invoke(obj, args);
			}
		}
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
