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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class SpyClassVisitor extends ClassVisitor {

    private SpyTransformer engine;
    private List<SpyDefinition> sdefs;
    private String className;

    private boolean isInterface;
    private String interfaces[];

    public SpyClassVisitor(SpyTransformer engine, String className,
                           List<SpyDefinition> sdefs, ClassVisitor cv) {
        super(Opcodes.V1_6, cv);
        this.engine = engine;
        this.className = className;
        this.sdefs = sdefs;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        isInterface = (access & ACC_INTERFACE) != 0;
        this.interfaces = Arrays.copyOf(interfaces, interfaces.length);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = createVisitor(access, name, desc, signature, exceptions);
        List<SpyContext> ctxs = new ArrayList<SpyContext>(sdefs.size());

        for (SpyDefinition sdef : sdefs) {
            if (sdef.match(className, name, desc, access)) {
                ctxs.add(engine.lookup(
                        new SpyContext(sdef, className, name, desc, access)));
            }
        }

        if (ctxs.size() > 0) {
            return new SpyMethodVisitor(access, name, desc, ctxs, mv);
        }

        return mv;
    }

    protected MethodVisitor createVisitor(int access, String name, String desc, String signature, String[] exceptions) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }
}
