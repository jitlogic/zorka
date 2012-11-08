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
import org.objectweb.asm.Type;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static com.jitlogic.zorka.spy.SpyConst.*;
import static com.jitlogic.zorka.spy.SpyLib.*;
import static com.jitlogic.zorka.spy.SpyUtil.max;

public class SpyMethodVisitor extends MethodVisitor {

    private final static String SUBMIT_CLASS = "com/jitlogic/zorka/spy/MainSubmitter";
    private final static String SUBMIT_METHOD = "submit";
    private final static String SUBMIT_DESC = "(III[Ljava/lang/Object;)V";

    private static boolean debug = false;

    private int access;
    private String methodName;
    private String methodDesc;

    private Type[] argTypes;
    private Type returnType;

    private int retValProbeSlot = 0;   // for both returns and errors
    private boolean hasReturnVal, hasErrorVal;

    private List<SpyContext> ctxs;

    private int stackDelta = 0;

    Label l_try_from = new Label();
    Label l_try_to = new Label();
    Label l_try_handler = new Label();


    public SpyMethodVisitor(int access, String methodName, String methodDesc, List<SpyContext> ctxs, MethodVisitor mv) {
        super(V1_6, mv);
        this.access = access;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.ctxs = ctxs;

        argTypes = Type.getArgumentTypes(methodDesc);
        returnType = Type.getReturnType(methodDesc);

        checkReturnVals();
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
            if (hasReturnVal) {
                emitFetchRetVal(returnType);
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

        if (hasErrorVal) {
            emitFetchRetVal(Type.getType(Object.class));
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
                for (SpyProbeElement spe : ctx.getSpyDefinition().getProbes(stage)) {
                    switch (spe.getArgType()) {
                        case FETCH_RETVAL:
                            hasReturnVal = true;
                            break;
                        case FETCH_ERROR:
                            hasErrorVal = true;
                            break;
                    } // switch()
                } // for (SpyProbeElement spe ....
            } // for (int stage ...
        } // for (SpyContext ctx ...

        if (hasReturnVal || hasErrorVal) {
            retValProbeSlot = argTypes.length + 1;
        } //

    } // checkReturnVals()


    private int emitProbe(int stage, SpyContext ctx) {
        SpyDefinition sdef = ctx.getSpyDefinition();
        List<SpyProbeElement> probeElements = sdef.getProbes(stage);

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
                SpyProbeElement element = probeElements.get(i);
                mv.visitInsn(DUP);
                emitLoadInt(i);
                sd = max(sd, emitProbeElement(stage, 0, probeElements.get(i)) + 6);
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


    private int emitProbeElement(int stage, int opcode, SpyProbeElement element) {
        switch (element.getArgType()) {
            case FETCH_TIME:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
                break;
            case FETCH_CLASS:
                String cn = "L"+element.getClassName().replace(".", "/") + ";";
                mv.visitLdcInsn(Type.getType(cn));
                break;
            case FETCH_THREAD:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
                break;
            case FETCH_ERROR:
            case FETCH_RETVAL:
                mv.visitVarInsn(ALOAD, retValProbeSlot);
                break;
            default:
                if (stage == ON_ENTER && element.getArgType() == 0 && "<init>".equals(methodName)) {
                    // TODO log warning
                    mv.visitInsn(ACONST_NULL);
                } else if (element.getArgType() >= 0) {
                    return emitFetchArgument(element);
                } else {
                    // TODO log warning
                    mv.visitInsn(ACONST_NULL);
                }
                break;
        }

        return 1;
    }


    private int emitFetchArgument(SpyProbeElement element) {

        if ((access & ACC_STATIC) == 0 && element.getArgType() == 0) {
            mv.visitVarInsn(ALOAD, element.getArgType());
            return 1;
        }

        int aoffs = (access & ACC_STATIC) == 0 ? 1 : 0;
        int aidx = element.getArgType() - aoffs;
        Type type = argTypes[aidx];
        int insn = type.getOpcode(ILOAD);

        for (int i = 0; i < aidx; i++) {
            aoffs += argTypes[i].getSize();
        }

        mv.visitVarInsn(insn, aoffs);
        emitAutoboxing(type);

        return 1;
    }


    private int emitFetchRetVal(Type type) {

        if (Type.VOID == type.getSort()) {
            mv.visitInsn(ACONST_NULL);
            return 1;
        }

        mv.visitInsn(DUP);
        emitAutoboxing(type);
        mv.visitVarInsn(ASTORE, retValProbeSlot);

        return type.getSize();
    }


    private void emitAutoboxing(Type type) {
        switch (type.getSort()) {
            case Type.INT:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer",
                        "valueOf", "(I)Ljava/lang/Integer;");
                break;
            case Type.LONG:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long",
                        "valueOf", "(J)Ljava/lang/Long;");
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float",
                        "valueOf", "(F)Ljava/lang/Float;");
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double",
                        "valueOf", "(D)Ljava/lang/Double;");
                break;
            case Type.SHORT:
                mv.visitInsn(I2S);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short",
                        "valueOf", "(S)Ljava/lang/Short;");
                break;
            case Type.BYTE:
                mv.visitInsn(I2B);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte",
                        "valueOf", "(B)Ljava/lang/Byte;");
                break;
            case Type.BOOLEAN:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean",
                        "valueOf", "(Z)Ljava/lang/Boolean;");
                break;
            case Type.CHAR:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character",
                        "valueOf", "(C)Ljava/lang/Character;");
                break;
        }
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
