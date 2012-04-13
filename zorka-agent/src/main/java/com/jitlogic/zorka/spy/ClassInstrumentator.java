package com.jitlogic.zorka.spy;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassInstrumentator extends ClassAdapter {

	private String className;
	private ZorkaSpy spy;
	
	private boolean isInterface;
	private String[] interfaces;
	
	public ClassInstrumentator(String className, ZorkaSpy spy, ClassVisitor cv) {
		super(cv);
		this.className = className;
		this.spy = spy;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		cv.visit(version, access, name, signature, superName, interfaces);
		isInterface = (access & ACC_INTERFACE) != 0;
		this.interfaces = interfaces;
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
