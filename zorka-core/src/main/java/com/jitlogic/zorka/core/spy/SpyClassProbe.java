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

/**
 * Fetches class in context of instrumented method called by application itself.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyClassProbe extends SpyProbe {

    /**
     * Class name
     */
    private String className;

    /**
     * Creates spy class probe
     *
     * @param dstField  destination field
     * @param className class name
     */
    public SpyClassProbe(String dstField, String className) {
        super(dstField);
        this.className = className;
    }


    @Override
    public int emit(SpyMethodVisitor mv, int stage, int opcode) {
        String cn = "L" + this.className.replace(".", "/") + ";";
        mv.visitLdcInsn(Type.getType(cn));
        return 1;
    }


    @Override
    public int hashCode() {
        return 17 * className.hashCode() + 31 * getDstField().hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SpyClassProbe)
                && ZorkaUtil.objEquals(getDstField(), ((SpyClassProbe) obj).getDstField())
                && ZorkaUtil.objEquals(className, ((SpyClassProbe) obj).className);
    }


    @Override
    public String toString() {
        return "SpyClassProbe(" + getDstField() + ", " + className + ")";
    }
}
