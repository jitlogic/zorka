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

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SimpleMethodInstrumentator extends MethodVisitor {

    //private final static String COLLECTOR_CLASS = "com/jitlogic/zorka/spy/MainCollector";
    private final static String COLLECTOR_CLASS = "com/jitlogic/zorka/bootstrap/AgentMain";

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
			mv.visitMethodInsn(INVOKESTATIC, COLLECTOR_CLASS, "logStart", "(J)V");
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
			mv.visitMethodInsn(INVOKESTATIC, COLLECTOR_CLASS, "logStart", "([Ljava/lang/Object;J)V");
		}
				
		mv.visitTryCatchBlock(l_try_from, l_try_to, l_try_handler, null);
		mv.visitLabel(l_try_from);		
	}
	
	
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitLdcInsn(id);
			mv.visitMethodInsn(INVOKESTATIC,
                    COLLECTOR_CLASS, "logCall", "(J)V");
		}
		mv.visitInsn(opcode);
	}
	
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitLabel(l_try_to);
		mv.visitLabel(l_try_handler);
		mv.visitLdcInsn(id);
		mv.visitMethodInsn(INVOKESTATIC, COLLECTOR_CLASS, "logError", "(J)V");
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
