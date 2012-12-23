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
import org.objectweb.asm.Type;

import static com.jitlogic.zorka.spy.SpyLib.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ILOAD;

/**
 *
 */
public class SpyProbeElement implements SpyProbe {

    private int argType;
    private String className;

    public SpyProbeElement(Object arg) {
        if (arg instanceof String) {
            className = (String)arg;
            argType = FETCH_CLASS;
        } else if (arg instanceof Integer) {
            argType = (Integer)arg;
        }

    }

    public int getArgType() {
        return argType;
    }

    public String getClassName() {
        return className;
    }

    public int emit(SpyMethodVisitor mv, int stage, int opcode) {
        switch (this.getArgType()) {
            case FETCH_TIME:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
                break;
            case FETCH_CLASS:
                String cn = "L"+this.getClassName().replace(".", "/") + ";";
                mv.visitLdcInsn(Type.getType(cn));
                break;
            case FETCH_THREAD:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
                break;
            case FETCH_ERROR:
            case FETCH_RETVAL:
                mv.visitVarInsn(ALOAD, mv.getRetValProbeSlot());
                break;
            case FETCH_NULL:
                mv.visitInsn(ACONST_NULL);
                break;
            default:
                if (stage == ON_ENTER && this.getArgType() == 0 && "<init>".equals(mv.getMethodName())) {
                    // TODO log warning
                    mv.visitInsn(ACONST_NULL);
                } else if (this.getArgType() >= 0) {
                    return emitFetchArgument(mv);
                } else {
                    // TODO log warning
                    mv.visitInsn(ACONST_NULL);
                }
                break;
        }

        return 1;
    }


    private int emitFetchArgument(SpyMethodVisitor mv) {

        if ((mv.getAccess() & ACC_STATIC) == 0 && this.getArgType() == 0) {
            mv.visitVarInsn(ALOAD, this.getArgType());
            return 1;
        }

        int aoffs = (mv.getAccess() & ACC_STATIC) == 0 ? 1 : 0;
        int aidx = this.getArgType() - aoffs;
        Type type = mv.getArgType(aidx); //argTypes[aidx];
        int insn = type.getOpcode(ILOAD);

        for (int i = 0; i < aidx; i++) {
            aoffs += mv.getArgType(i).getSize();
        }

        mv.visitVarInsn(insn, aoffs);
        emitAutoboxing(mv, type);

        return 1;
    }


    public static int emitFetchRetVal(SpyMethodVisitor mv, Type type) {

        if (Type.VOID == type.getSort()) {
            mv.visitInsn(ACONST_NULL);
            return 1;
        }

        mv.visitInsn(DUP);
        emitAutoboxing(mv, type);
        mv.visitVarInsn(ASTORE, mv.getRetValProbeSlot());

        return type.getSize();
    }


    public static void emitAutoboxing(SpyMethodVisitor mv, Type type) {
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

}
