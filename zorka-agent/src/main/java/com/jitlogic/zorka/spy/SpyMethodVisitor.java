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

import com.jitlogic.zorka.spy.probes.SpyProbe;
import com.jitlogic.zorka.spy.probes.SpyReturnProbe;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static com.jitlogic.zorka.spy.SpyLib.*;
import static java.lang.Math.max;

public class SpyMethodVisitor extends MethodVisitor {

    private final static String SUBMIT_CLASS = "com/jitlogic/zorka/spy/MainSubmitter";
    private final static String SUBMIT_METHOD = "submit";
    private final static String SUBMIT_DESC = "(III[Ljava/lang/Object;)V";

    private static boolean debug = false;

    private int access;
    private String methodName;

    private Type[] argTypes;
    private Type returnType;

    private int retValProbeSlot = 0;   // for both returns and errors

    private SpyProbe returnProbe;//, errorProbe;

    private List<SpyContext> ctxs;

    private int stackDelta = 0;

    private Label l_try_from = new Label();
    private Label l_try_to = new Label();
    private Label l_try_handler = new Label();


    public SpyMethodVisitor(int access, String methodName, String methodDesc, List<SpyContext> ctxs, MethodVisitor mv) {
        super(V1_6, mv);
        this.access = access;
        this.methodName = methodName;
        this.ctxs = ctxs;

        argTypes = Type.getArgumentTypes(methodDesc);
        returnType = Type.getReturnType(methodDesc);

        checkReturnVals();
    }


    public String getMethodName() {
        return methodName;
    }

    public int getRetValProbeSlot() {
        return retValProbeSlot;
    }

    public int getAccess() {
        return access;
    }

    public Type getArgType(int idx) {
        return argTypes[idx];
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        // ON_ENTER probes are inserted here
        for (SpyContext ctx : ctxs) {
            if (ctx.getSpyDefinition().getProbes(ON_ENTER).size() > 0) {
                stackDelta = max(stackDelta, emitProbe(ON_ENTER, ctx));
            }
        }

        mv.visitTryCatchBlock(l_try_from, l_try_to, l_try_handler, null);
        mv.visitLabel(l_try_from);
    }


    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            // ON_RETURN probes are inserted here
            if (returnProbe != null) {
                returnProbe.emitFetchRetVal(this, returnType);
            }
            for (int i = ctxs.size()-1; i >= 0; i--) {
                SpyContext ctx = ctxs.get(i);
                if (getSubmitFlags(ON_ENTER, ctx.getSpyDefinition()) == SF_NONE ||
                    ctx.getSpyDefinition().getProbes(ON_RETURN).size() > 0) {
                    stackDelta = max(stackDelta, emitProbe(ON_RETURN, ctx));
                }
            }
        }
        mv.visitInsn(opcode);
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(l_try_to);
        mv.visitLabel(l_try_handler);

        if (returnProbe != null) {
            returnProbe.emitFetchRetVal(this, Type.getType(Object.class));
        }

        for (int i = ctxs.size()-1; i >= 0; i--) {
            SpyContext ctx = ctxs.get(i);
            if (getSubmitFlags(ON_ENTER, ctx.getSpyDefinition()) == SF_NONE ||
                ctx.getSpyDefinition().getProbes(ON_ERROR).size() > 0) {
                stackDelta = max(stackDelta, emitProbe(ON_ERROR, ctx));
            }
        }

        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack + stackDelta, max(maxLocals, retValProbeSlot+1));
    }


    public static int getSubmitFlags(int stage, SpyDefinition sdef) {

        if (stage == ON_ENTER) {
            return (sdef.getProbes(ON_RETURN).size() == 0
                    && sdef.getProbes(ON_ERROR).size() == 0)
                    ? SF_IMMEDIATE : SF_NONE;
        } else {
            return (sdef.getProbes(ON_ENTER).size() == 0)
                    ? SF_IMMEDIATE : SF_FLUSH;
        }
    }


    private void checkReturnVals() {

        for (SpyContext ctx : ctxs) {
            for (int stage : new int[] { ON_ERROR, ON_RETURN }) {
                for (SpyProbe spe : ctx.getSpyDefinition().getProbes(stage)) {
                    if (spe instanceof SpyReturnProbe) {
                        returnProbe = spe;
                    }
                } // for (SpyProbeElement spe ....
            } // for (int stage ...
        } // for (SpyContext ctx ...

        if (returnProbe != null) {
            retValProbeSlot = argTypes.length + 1;
        } //

    } // checkReturnVals()


    private int emitProbe(int stage, SpyContext ctx) {
        SpyDefinition sdef = ctx.getSpyDefinition();
        List<SpyProbe> probeElements = sdef.getProbes(stage);

        int submitFlags = getSubmitFlags(stage, sdef);

        // Put first 3 arguments of MainSubmitter.submit() onto stack
        emitLoadInt(stage);
        emitLoadInt(ctx.getId());
        emitLoadInt(submitFlags);

        int sd = 3;

        // Create an array with fetched data (or push null)
        if (probeElements.size() > 0) {
            emitLoadInt(probeElements.size());
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            for (int i = 0; i < probeElements.size(); i++) {
                SpyProbe element = probeElements.get(i);
                mv.visitInsn(DUP);
                emitLoadInt(i);
                //sd = max(sd, emitProbeElement(stage, 0, probeElements.get(i)) + 6);
                sd = max(sd, probeElements.get(i).emit(this, stage, 0) + 6);
                mv.visitInsn(AASTORE);
            }
        } else {
            mv.visitInsn(ACONST_NULL);
            sd++;
        }

        // Call MainSubmitter.submit()
        mv.visitMethodInsn(INVOKESTATIC, SUBMIT_CLASS, SUBMIT_METHOD, SUBMIT_DESC);

        return sd;
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
            mv.visitFieldInsn(GETSTATIC,
                    "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(msg);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        }
    }


}
