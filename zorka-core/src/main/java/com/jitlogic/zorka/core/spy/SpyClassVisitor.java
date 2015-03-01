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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Traverses class file and instruments selected method according to supplied spy definitions.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyClassVisitor extends ClassVisitor {

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Parent class transformer
     */
    private SpyClassTransformer transformer;

    /**
     * Defining class loader.
     */
    private ClassLoader classLoader;

    /**
     * List of spy definitions to be applied (visitor will only check for method matches, not class matches!)
     */
    private List<SpyDefinition> sdefs;

    /**
     * List of matchers used by tracer generation code.
     */
    private Tracer tracer;

    /**
     * Name of instrumented class
     */
    private String className;

    private List<String> classInterfaces;

    /**
     * List of class annotations encountered when traversing class
     */
    private List<String> classAnnotations = new ArrayList<String>();

    private SymbolRegistry symbolRegistry;

    private boolean recursive;

    /**
     * Creates Spy class visitor.
     *
     * @param transformer parent class transformer
     * @param className   class name
     * @param sdefs       list of spy definitions to be applied
     * @param tracer      reference to tracer
     * @param cv          output class visitor (typically ClassWriter)
     */
    public SpyClassVisitor(SpyClassTransformer transformer, ClassLoader classLoader, SymbolRegistry symbolRegistry,
                           String className,
                           List<SpyDefinition> sdefs, Tracer tracer, ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
        this.transformer = transformer;
        this.classLoader = classLoader;
        this.symbolRegistry = symbolRegistry;
        this.className = className;
        this.sdefs = sdefs;
        this.tracer = tracer;

        for (SpyDefinition sdef : sdefs) {
            for (SpyMatcher matcher : sdef.getMatcherSet().getMatchers()) {
                if (matcher.hasFlags(SpyMatcher.RECURSIVE)) {
                    recursive = true;
                }
            }
        }
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);

        this.classInterfaces = new ArrayList(interfaces.length);
        for (String ifname : interfaces) {
            String interfaceClass = ifname.replace('/', '.');
            this.classInterfaces.add(interfaceClass);
            if (recursive) {

                try {
                    recursiveInterfaceScan(interfaceClass);
                } catch (Exception e) {
                    log.error(ZorkaLogger.ZSP_CONFIG, "Cannot (recursively) load class: " + interfaceClass, e);
                }

            }
        }

        if ((recursive) && !"java/lang/Object".equals(superName)) {
            try {
                recursiveInterfaceScan(superName.replace('/', '.'));
            } catch (Exception e) {
                log.error(ZorkaLogger.ZSP_CONFIG, "Cannot (recursively) load class: " + superName, e);
            }
        }

    }


    private void recursiveInterfaceScan(String className) throws ClassNotFoundException {
        Class<?> clazz = classLoader.loadClass(className);
        for (Class<?> ifc : clazz.getInterfaces()) {
            String ifname = ifc.getName();
            if (!classInterfaces.contains(ifname)) {
                classInterfaces.add(ifname);
                recursiveInterfaceScan(ifname);
            }
        }
        if (!clazz.isInterface() && clazz.getSuperclass() != Object.class) {
            recursiveInterfaceScan(clazz.getSuperclass().getName());
        }
    }


    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        classAnnotations.add(desc.replace("/", "."));
        return super.visitAnnotation(desc, visible);
    }


    @Override
    public MethodVisitor visitMethod(int access, String methodName, String methodDesc,
                                     String methodSignature, String[] exceptions) {

        log.debug(ZorkaLogger.ZSP_METHOD_TRC, "Encountered method: " + className + "." + methodName + " " + methodDesc);

        MethodVisitor mv = createVisitor(access, methodName, methodDesc, methodSignature, exceptions);
        List<SpyContext> ctxs = new ArrayList<SpyContext>(sdefs.size() + 2);

        boolean m = true;

        for (SpyDefinition sdef : sdefs) {
            if (sdef.getMatcherSet().methodMatch(className, classAnnotations, classInterfaces,
                    access, methodName, methodDesc, null)) {
                log.debug(ZorkaLogger.ZSP_METHOD_DBG, "Instrumenting method (full SPY): " + className + "." + methodName + " " + methodDesc);
                ctxs.add(transformer.lookup(
                        new SpyContext(sdef, className, methodName, methodDesc, access)));
            }

            if (sdef.getMatcherSet().hasMethodAnnotations()) {
                m = false;
            }
        }

        boolean doTrace = tracer.getMatcherSet().methodMatch(className, classAnnotations, classInterfaces,
                access, methodName, methodDesc, null) || (tracer.isTraceSpyMethods() && ctxs.size() > 0);

        if (doTrace && ZorkaLogger.isLogMask(ZorkaLogger.ZTR_INSTRUMENT_METHOD)) {
            log.debug(ZorkaLogger.ZTR_INSTRUMENT_METHOD, "Instrumenting method (for trace): "
                    + className + "." + methodName + " " + methodDesc);
        }

        if (ctxs.size() > 0 || doTrace) {
            return new SpyMethodVisitor(m, doTrace ? symbolRegistry : null, className,
                    classAnnotations, classInterfaces, access, methodName, methodDesc, ctxs, mv);
        }

        return mv;
    }

    /**
     * Creates method visitor for given method.
     *
     * @param access     method access flags
     * @param name       method name
     * @param desc       method descriptor
     * @param signature  method signature
     * @param exceptions names of thrown exceptions
     * @return method visitor
     */
    protected MethodVisitor createVisitor(int access, String name, String desc, String signature, String[] exceptions) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }
}
