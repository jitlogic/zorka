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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.SIPUSH;

public class SpyMethodVisitor extends MethodVisitor {

    private final static String SUBMIT_CLASS = "com/jitlogic/zorka/vmsci/MainSubmitter";
    private final static String SUBMIT_METHOD = "submit";
    private final static String SUBMIT_DESC = "(IIZ[Ljava/lang/Object;)V";

    private static boolean debug = false;
    private List<InstrumentationContext> ctxs;
    private int stackDelta = 0, localDelta = 0;

    public SpyMethodVisitor(List<InstrumentationContext> ctxs, MethodVisitor mv) {
        super(Opcodes.V1_6, mv);
        this.ctxs = ctxs;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        for (InstrumentationContext ctx : ctxs) {
            if (ctx.getSpyDefinition().getProbes(SpyDefinition.ON_ENTER).size() > 0) {
                stackDelta = max(stackDelta, emitProbe(SpyDefinition.ON_ENTER, ctx));
            }
        }

    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + stackDelta, maxLocals + localDelta);
    }

    private static int max(int x, int y) {
        return x > y ? x : y;
    }

    private int emitProbe(int stage, InstrumentationContext ctx) {
        SpyDefinition sdef = ctx.getSpyDefinition();
        List<SpyProbeElement> probeElements = sdef.getProbes(stage);
        boolean submitNow = (stage != SpyDefinition.ON_ENTER) ||
            (sdef.getProbes(SpyDefinition.ON_EXIT).size()+sdef.getProbes(SpyDefinition.ON_ERROR).size() == 0);

        // Put first 3 arguments of MainSubmitter.submit() onto stack
        emitLoadInt(stage);
        emitLoadInt(ctx.getId());
        emitLoadInt(submitNow ? 1 : 0);

        int sd = 3;

        // Create an array with fetched data (or push null)
        if (probeElements.size() > 0) {
            emitLoadInt(probeElements.size());
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            for (int i = 0; i < probeElements.size(); i++) {
                SpyProbeElement element = probeElements.get(i);
                mv.visitInsn(Opcodes.DUP);
                emitLoadInt(i);
                sd = max(sd, emitProbeElement(stage, 0, probeElements.get(i)) + 5);
                mv.visitInsn(Opcodes.AASTORE);
            }
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
            sd++;
        }

        // Call MainSubmitter.submit()
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, SUBMIT_CLASS, SUBMIT_METHOD, SUBMIT_DESC);

        return sd;
    }


    private int emitProbeElement(int stage, int opcode, SpyProbeElement element) {
        switch (element.getArgType()) {
            case SpyProbeElement.FETCH_TIME:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
                break;
            case SpyProbeElement.FETCH_CLASS:
                throw new NotImplementedException();
            case SpyProbeElement.FETCH_THREAD:
                throw new NotImplementedException();
            case SpyProbeElement.FETCH_ERROR:
                throw new NotImplementedException();
            case SpyProbeElement.FETCH_RET_VAL:
                throw new NotImplementedException();
            default:
                if (element.getArgType() >= 0) {
                    mv.visitVarInsn(Opcodes.ALOAD, element.getArgType());
                }
                break;
        }

        return 1;
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


    private void emitDebugPrint(String msg) {
        if (debug) {
            mv.visitFieldInsn(Opcodes.GETSTATIC,
                    "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(msg);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        }
    }


}
