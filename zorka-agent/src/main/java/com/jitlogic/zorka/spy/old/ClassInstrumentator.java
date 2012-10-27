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

package com.jitlogic.zorka.spy.old;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

public class ClassInstrumentator extends ClassVisitor {

	private String className;
	private ZorkaSpy spy;
	
	private boolean isInterface;
	private String[] interfaces;
	
	public ClassInstrumentator(String className, ZorkaSpy spy, ClassVisitor cv) {
		super(Opcodes.V1_6, cv);
		this.className = className;
		this.spy = spy;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		cv.visit(version, access, name, signature, superName, interfaces);
		isInterface = (access & ACC_INTERFACE) != 0;
		this.interfaces = Arrays.copyOf(interfaces, interfaces.length);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		if (!isInterface && mv != null && !name.equals("<init>")) {
			DataCollector collector = spy.lookup(className, name, signature, interfaces);
			if (collector != null) {
				mv = collector.getAdapter(mv);
			}
		}
		return mv;
	}
	
	
}
