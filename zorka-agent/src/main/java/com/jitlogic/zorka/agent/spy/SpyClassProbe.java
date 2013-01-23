/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.agent.spy;

import org.objectweb.asm.Type;

/**
 * Fetches class in context of instrumented method called by application itself.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyClassProbe extends SpyProbe {

    /** Class name */
    private String className;

    /**
     * Creates spy class probe
     *
     * @param className class name
     *
     * @param dstField destination field
     */
    public SpyClassProbe(String className, String dstField) {
        super(dstField);
        this.className = className;
    }


    @Override
    public int emit(SpyMethodVisitor mv, int stage, int opcode) {
        String cn = "L"+this.className.replace(".", "/") + ";";
        mv.visitLdcInsn(Type.getType(cn));
        return 1;
    }
}
