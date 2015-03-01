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

import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.objectweb.asm.Type;

import static com.jitlogic.zorka.core.spy.SpyLib.ON_ENTER;
import static org.objectweb.asm.Opcodes.*;

/**
 * Fetches method argument.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyArgProbe extends SpyProbe {

    /**
     * Argument index
     */
    private int argIndex;


    /**
     * Creates new argument fetching probe
     *
     * @param argIdx   argument index
     * @param dstField destination field
     */
    public SpyArgProbe(int argIdx, String dstField) {
        super(dstField);
        this.argIndex = argIdx;
    }


    @Override
    public int emit(SpyMethodVisitor mv, int stage, int opcode) {
        if (stage == ON_ENTER && argIndex == 0 && "<init>".equals(mv.getMethodName())) {
            // TODO log warning
            mv.visitInsn(ACONST_NULL);
        } else if (argIndex >= 0) {

            if ((mv.getAccess() & ACC_STATIC) == 0 && argIndex == 0) {
                mv.visitVarInsn(ALOAD, argIndex);
                return 1;
            }

            int aoffs = (mv.getAccess() & ACC_STATIC) == 0 ? 1 : 0;
            int aidx = argIndex - aoffs;
            Type type = mv.getArgType(aidx);
            int insn = type.getOpcode(ILOAD);

            for (int i = 0; i < aidx; i++) {
                aoffs += mv.getArgType(i).getSize();
            }

            mv.visitVarInsn(insn, aoffs);
            emitAutoboxing(mv, type);

            return 1;
        } else {
            // TODO log warning
            mv.visitInsn(ACONST_NULL);
        }

        return 1;
    }


    @Override
    public int hashCode() {
        return 31 * getDstField().hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SpyArgProbe)
                && ZorkaUtil.objEquals(getDstField(), ((SpyArgProbe) obj).getDstField()) &&
                argIndex == ((SpyArgProbe) obj).argIndex;
    }


    @Override
    public String toString() {
        return "SpyArgProbe(" + getDstField() + ", " + argIndex + ")";
    }

}
