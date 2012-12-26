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

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static com.jitlogic.zorka.spy.SpyLib.SPD_METHODALL;
import static com.jitlogic.zorka.spy.SpyLib.SPD_METHODXFORM;

public class SpyClassVisitor extends ClassVisitor {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SpyClassTransformer engine;
    private List<SpyDefinition> sdefs;
    private String className;


    private boolean isInterface;
    private String interfaces[];


    public SpyClassVisitor(SpyClassTransformer engine, String className,
                           List<SpyDefinition> sdefs, ClassVisitor cv) {
        super(V1_6, cv);
        this.engine = engine;
        this.className = className;
        this.sdefs = sdefs;
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        isInterface = (access & ACC_INTERFACE) != 0;
        this.interfaces = ZorkaUtil.copyArray(interfaces);
    }


    @Override
    public MethodVisitor visitMethod(int access, String methodName, String methodDesc,
                                     String methodSignature, String[] exceptions) {

        if (SpyInstance.isDebugEnabled(SPD_METHODALL)) {
            log.debug("Encountered method: " + className + "." + methodName + " " + methodDesc);
        }

        MethodVisitor mv = createVisitor(access, methodName, methodDesc, methodSignature, exceptions);
        List<SpyContext> ctxs = new ArrayList<SpyContext>(sdefs.size()+2);

        for (SpyDefinition sdef : sdefs) {
            if (sdef.match(className, methodName, methodDesc, access)) {
                if (SpyInstance.isDebugEnabled(SPD_METHODXFORM)) {
                    log.debug("Instrumenting method: " + className + "." + methodName + " " + methodDesc);
                }
                ctxs.add(engine.lookup(
                        new SpyContext(sdef, className, methodName, methodDesc, access)));
            }
        }

        if (ctxs.size() > 0) {
            return new SpyMethodVisitor(access, methodName, methodDesc, ctxs, mv);
        }

        return mv;
    }


    protected MethodVisitor createVisitor(int access, String name, String desc, String signature, String[] exceptions) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }
}
