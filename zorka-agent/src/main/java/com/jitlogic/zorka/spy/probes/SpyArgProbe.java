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
package com.jitlogic.zorka.spy.probes;

import com.jitlogic.zorka.spy.SpyMethodVisitor;
import org.objectweb.asm.Type;

import static com.jitlogic.zorka.api.SpyLib.ON_ENTER;
import static org.objectweb.asm.Opcodes.*;

public class SpyArgProbe extends SpyProbe {

    private int argType;

    public SpyArgProbe(int arg, String dstKey) {
        super(dstKey);
        this.argType = arg;
    }

    public int emit(SpyMethodVisitor mv, int stage, int opcode) {
        if (stage == ON_ENTER && argType == 0 && "<init>".equals(mv.getMethodName())) {
            // TODO log warning
            mv.visitInsn(ACONST_NULL);
        } else if (argType >= 0) {
            return emitFetchArgument(mv);
        } else {
            // TODO log warning
            mv.visitInsn(ACONST_NULL);
        }

        return 1;
    }

    protected int emitFetchArgument(SpyMethodVisitor mv) {

        if ((mv.getAccess() & ACC_STATIC) == 0 && argType == 0) {
            mv.visitVarInsn(ALOAD, argType);
            return 1;
        }

        int aoffs = (mv.getAccess() & ACC_STATIC) == 0 ? 1 : 0;
        int aidx = argType - aoffs;
        Type type = mv.getArgType(aidx);
        int insn = type.getOpcode(ILOAD);

        for (int i = 0; i < aidx; i++) {
            aoffs += mv.getArgType(i).getSize();
        }

        mv.visitVarInsn(insn, aoffs);
        emitAutoboxing(mv, type);

        return 1;
    }


}
