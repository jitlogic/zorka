package com.jitlogic.zorka.spy;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class SimpleMethodInstrumentator extends MethodAdapter {

	private long id;

	Label l_try_from = new Label();
	Label l_try_to = new Label();
	Label l_try_handler = new Label();
	
	public SimpleMethodInstrumentator(MethodVisitor mv, long id) {
		super(mv);
		this.id = id;
	}
	
	
	@Override
	public void visitCode() {
		mv.visitCode();
		mv.visitLdcInsn(id);
		mv.visitMethodInsn(INVOKESTATIC, 
			"com/jitlogic/zorka/spy/MainCollector", "logStart", "(J)V");
				
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
		mv.visitMaxs(maxStack+5, maxLocals);
		
	}

	@SuppressWarnings("unused")
	private void injectPrintf(String msg) {
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn(msg);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
	}
}
