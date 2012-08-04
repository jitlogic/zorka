package com.jitlogic.zorka.spy;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SimpleMethodInstrumentator extends MethodVisitor {

	private long id;
	private int[] args;

	Label l_try_from = new Label();
	Label l_try_to = new Label();
	Label l_try_handler = new Label();
	
	public SimpleMethodInstrumentator(MethodVisitor mv, long id, int...args) {
		super(Opcodes.V1_6, mv);
		this.id = id;
		this.args = args;
	}
	
	
	private void emitLoadInt(int v) {
		if (v >= 0 && v <= 5) {
			mv.visitInsn(ICONST_0 + v);
		} else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
			mv.visitIntInsn(BIPUSH, v);
		} else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
			mv.visitIntInsn(SIPUSH, v);
		} else {
			mv.visitLdcInsn(new Integer(v));
		}
	}
	
	
	@Override
	public void visitCode() {
		mv.visitCode();
		
		if (args.length == 0) {
			mv.visitLdcInsn(id);
			mv.visitMethodInsn(INVOKESTATIC, 
					"com/jitlogic/zorka/spy/MainCollector", "logStart", "(J)V");
		} else {
			emitLoadInt(args.length);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			for (int i = 0; i < args.length; i++) {
				mv.visitInsn(DUP);
				emitLoadInt(i);
				mv.visitVarInsn(ALOAD, args[i]);
				mv.visitInsn(AASTORE);
			}
			mv.visitLdcInsn(id);
			mv.visitMethodInsn(INVOKESTATIC, 
				"com/jitlogic/zorka/spy/MainCollector", 
				"logStart", "([Ljava/lang/Object;J)V");			
		}
				
		mv.visitTryCatchBlock(l_try_from, l_try_to, l_try_handler, null);
		mv.visitLabel(l_try_from);		
	}
	
	
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitLdcInsn(id);
			mv.visitMethodInsn(INVOKESTATIC,
				"com/jitlogic/zorka/spy/MainCollector", "logCall", "(J)V");
		}
		mv.visitInsn(opcode);
	}
	
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitLabel(l_try_to);
		mv.visitLabel(l_try_handler);
		mv.visitLdcInsn(id);
		mv.visitMethodInsn(INVOKESTATIC,
			"com/jitlogic/zorka/spy/MainCollector", "logError", "(J)V");		
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack+5+args.length, maxLocals);
		
	}

	@SuppressWarnings("unused")
	private void injectPrintf(String msg) {
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn(msg);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
	}
}
