/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class MethodInstrumenter extends MethodVisitor {

    private static boolean debug = false;

    private List<SpyDefinition> sdefs;

    Label l_try_from = new Label();
    Label l_try_to = new Label();
    Label l_try_handler = new Label();

    int stackDelta = 0;


    public MethodInstrumenter(MethodVisitor mv, List<SpyDefinition> sdefs) {
        super(Opcodes.V1_6, mv);
        this.sdefs = sdefs;
    }


    @Override
    public void visitCode() {
        emitEntryCode();
    }


    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
            emitExitCode();
        }
        mv.visitInsn(opcode);
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(l_try_to);
        mv.visitLabel(l_try_handler);
        emitErrorCode();
        mv.visitMaxs(maxStack + stackDelta, maxLocals);
    }


    private void emitEntryCode() {
        for (SpyDefinition sdef : sdefs) {
            // TODO
        }
    }


    private void emitExitCode() {
        // TODO
    }


    private void emitErrorCode() {
        // TODO
    }


    private void emitDebugPrint(String msg) {
        if (debug) {
            mv.visitFieldInsn(Opcodes.GETSTATIC,
                "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(msg);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        }
    }


    public static void setDebug() {
        debug = true;
    }
}
