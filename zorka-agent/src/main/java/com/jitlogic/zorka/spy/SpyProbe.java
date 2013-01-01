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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.spy.SpyDefArg;
import com.jitlogic.zorka.spy.SpyMethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public abstract class SpyProbe implements SpyDefArg {

    private String key;

    public SpyProbe(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public abstract int emit(SpyMethodVisitor mv, int stage, int opcode);


    public int emitFetchRetVal(SpyMethodVisitor mv, Type type) {

        if (Type.VOID == type.getSort()) {
            mv.visitInsn(ACONST_NULL);
            return 1;
        }

        mv.visitInsn(DUP);
        emitAutoboxing(mv, type);
        mv.visitVarInsn(ASTORE, mv.getRetValProbeSlot());

        return type.getSize();
    }


    public void emitAutoboxing(SpyMethodVisitor mv, Type type) {
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
