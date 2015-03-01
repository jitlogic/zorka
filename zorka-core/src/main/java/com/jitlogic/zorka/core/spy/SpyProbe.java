/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.spy;

import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * Abstract class representing spy probe. Implements common methods used by probes.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public abstract class SpyProbe implements SpyDefArg {


    /** Field name fetched value will be saved in spy record */
    private String dstField;


    /**
     * Creates new spy probe.
     *
     * @param dstField destination field name
     */
    public SpyProbe(String dstField) {
        this.dstField = dstField;
    }


    /**
     * Returns name fetched data will be saved as
     *
     * @return field name
     */
    public String getDstField() {
        return dstField;
    }


    /**
     * Emits probe bytecode.
     *
     * @param mv output method visitor
     *
     * @param stage point in method code probe is being inserted (entry point,
     *
     * @param opcode
     *
     * @return number of JVM stack slots emitted code consumes
     */
    public abstract int emit(SpyMethodVisitor mv, int stage, int opcode);


    /**
     * Fetches return value or thrown exception. If return value is of basic type, it is automatically boxed.
     *
     * @param mv output method visitor
     *
     * @param type return data type
     *
     * @return number of JVM stack slots emitted code consumes
     */
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


    /**
     * Emits code boxing value of basic type.
     *
     * @param mv output method visitor
     *
     * @param type input data type (must be basic type!)
     */
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
