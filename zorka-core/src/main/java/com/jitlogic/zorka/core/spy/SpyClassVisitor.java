/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Logger log = LoggerFactory.getLogger(this.getClass());

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

    private boolean bytecodeWasModified = false;

    private List<String> superclasses;

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
                           String className, List<SpyDefinition> sdefs, Tracer tracer, ClassVisitor cv) {
        super(Opcodes.ASM6, cv);
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
                if (matcher.hasFlags(SpyMatcher.BY_SUPERCLASS)) {
                    superclasses = new ArrayList<String>();
                }
            }
        }
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);

        this.classInterfaces = new ArrayList(interfaces.length);


        if (superclasses != null) {
            String superclass = superName.replace('/', '.');
            superclasses.add(superclass);

            if (recursive) {
                try {
                    Class<?> clazz = classLoader.loadClass(superclass);
                    while (!"java.lang.Object".equals(clazz.getName())) {
                        clazz = clazz.getSuperclass();
                        superclasses.add(clazz.getName());
                    }
                } catch (Exception e) {
                    log.error("Cannot (recursively) load class: " + superclass, e);
                }
            }

        }

        for (String ifname : interfaces) {
            String interfaceClass = ifname.replace('/', '.');
            this.classInterfaces.add(interfaceClass);
            if (recursive) {

                try {
                    recursiveInterfaceScan(interfaceClass);
                } catch (Exception e) {
                    log.error("Cannot (recursively) load class: " + interfaceClass, e);
                }

            }
        }

        if ((recursive) && !"java/lang/Object".equals(superName)) {
            try {
                recursiveInterfaceScan(superName.replace('/', '.'));
            } catch (Exception e) {
                log.error("Cannot (recursively) load class: " + superName, e);
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


    private int lastArgIndex(int access, String methodSignature) {
        return Type.getArgumentTypes(methodSignature).length + ((0 != (access & 8)) ? 0 : 1) - 1;
    }


    @Override
    public MethodVisitor visitMethod(int access, String methodName, String methodDesc,
                                     String methodSignature, String[] exceptions) {

        if (log.isTraceEnabled()) {
            log.trace("Encountered method: " + className + "." + methodName + " " + methodDesc);
        }

        MethodVisitor mv = createVisitor(access, methodName, methodDesc, methodSignature, exceptions);
        List<SpyContext> ctxs = new ArrayList<SpyContext>(sdefs.size() + 2);

        boolean m = true;

        for (SpyDefinition sdef : sdefs) {
            if (sdef.getMatcherSet().methodMatch(className, superclasses, classAnnotations, classInterfaces,
                    access, methodName, methodDesc, null)) {
                if (sdef.getLastArgIndex() > lastArgIndex(access, methodDesc)) {
                    log.error("Cannot instrument method " + className + "." + methodName
                        + "(). SpyDef " + sdef.getName() + " refers to argument(s) beyond method argument list.");
                } else {
                    log.info("Instrumenting method for " + sdef.getName() + ": " + className + "." + methodName + " " + methodDesc);
                    ctxs.add(transformer.lookup(
                        new SpyContext(sdef, className, methodName, methodDesc, access)));
                }
            }

            if (sdef.getMatcherSet().hasMethodAnnotations()) {
                m = false;
            }
        }

        boolean doTrace = tracer.getMatcherSet().methodMatch(className, superclasses, classAnnotations, classInterfaces,
                access, methodName, methodDesc, null) || (tracer.isTraceSpyMethods() && ctxs.size() > 0);

        if (doTrace && log.isDebugEnabled()) {
            log.debug("Instrumenting method (for trace): " + className + "." + methodName + " " + methodDesc);
        }

        if (ctxs.size() > 0 || doTrace) {
            bytecodeWasModified = true;
            return new SpyMethodVisitor(m, doTrace ? symbolRegistry : null, className, superclasses,
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

    public boolean wasBytecodeModified() {
        return bytecodeWasModified;
    }
}
