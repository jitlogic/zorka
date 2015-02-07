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

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * Fetches current thread object in context of method called by application code
 * (that is, application thread).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyThreadProbe extends SpyProbe {

    /**
     * Creates spy thread probe
     *
     * @param dstField destination field
     */
    public SpyThreadProbe(String dstField) {
        super(dstField);
    }


    @Override
    public int emit(SpyMethodVisitor mv, int stage, int opcode) {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
        return 1;
    }


    @Override
    public int hashCode() {
        return 31 * getDstField().hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SpyThreadProbe)
                && ZorkaUtil.objEquals(getDstField(), ((SpyThreadProbe) obj).getDstField());
    }


    @Override
    public String toString() {
        return "SpyThreadProbe(" + getDstField() + ")";
    }

}
