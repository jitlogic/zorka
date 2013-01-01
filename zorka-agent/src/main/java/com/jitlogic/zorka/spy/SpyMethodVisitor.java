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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static com.jitlogic.zorka.api.SpyLib.*;
import static java.lang.Math.max;

/**
 * Spy method visitor is responsible for actual instrumenting. It is instantiated when Spy engine finds method that
 * has to be instrumented and inserts all probes required by passed spy contexts. If some spy contexts use method
 * attribute matches to identify methods to be instrumented, spy method visitor will look for method annotatinos and
 * decide whether method actually has to be instrumented or not.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyMethodVisitor extends MethodVisitor {

    /** Class that receives data from probes */
    private final static String SUBMIT_CLASS = "com/jitlogic/zorka/spy/MainSubmitter";

    /** Name of a method that will be called from probes */
    private final static String SUBMIT_METHOD = "submit";

    /** Descriptor of a method that will be called from probes */
    private final static String SUBMIT_DESC = "(III[Ljava/lang/Object;)V";

    /** Debug mode */
    private static boolean debug = false;

    /** Access flags of (instrumented) method */
    private final int access;

    /** Name of (instrumented) metod */
    private final String methodName;

    /** Argument types of (instrumented) method */
    private final Type[] argTypes;

    /** Return type of (instrumented) method */
    private final Type returnType;

    /** Slot in method stack for retaining return values (or exception objects) */
    private int retValProbeSlot = 0;

    /** Return probe (error probe) found in any of supplied contexts. It is actually only needed to
     *  mark if return/error value is actually neede and generate parts of error/return fetch code that
     *  has to be executed before other probes on return/error paths.
     */
    private SpyProbe returnProbe;

    /**
     * List of Spy Contexts that will receive events on this method execution. Spy contexts contain spy
     * definitions that in turn contain lists of probe definitions.
     */
    private List<SpyContext> ctxs;

    /** How many elements have to be added to JVM stack by instrumented code. Used in visitMaxs() method. */
    private int stackDelta = 0;

    /** Labels used for enclosing method code into try..finally block to intercept thrown exceptions. */
    private final Label lTryFrom = new Label(), lTryTo = new Label(), lTryHandler = new Label();

    /** Boolean flag used when method annotation matching is required */
    private boolean matches;

    /**
     * Standard constructor.
     *
     * @param matches true if visitor should always instrument or false if it should check for annotations
     *
     * @param access method access flags
     *
     * @param methodName method name
     *
     * @param methodDesc method descriptor
     *
     * @param ctxs spy contexts interested in receiving data from this visitor
     *
     * @param mv method visitor (next in processing chain)
     */
    public SpyMethodVisitor(boolean matches, int access, String methodName, String methodDesc, List<SpyContext> ctxs, MethodVisitor mv) {
        super(V1_6, mv);
        this.matches = matches;
        this.access = access;
        this.methodName = methodName;
        this.ctxs = ctxs;

        argTypes = Type.getArgumentTypes(methodDesc);
        returnType = Type.getReturnType(methodDesc);

        checkReturnVals();
    }


    /**
     * Returns method name
     *
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns slot number in method stack that will retain return value (or thrown exception)
     *
     * @return slot number with return value (thrown exception)
     */
    public int getRetValProbeSlot() {
        return retValProbeSlot;
    }

    /**
     * Returns method access flags
     *
     * @return access flags of instrumented method
     */
    public int getAccess() {
        return access;
    }

    /**
     * Returns type of method argument at given position.
     *
     * @param idx argument index
     *
     * @return argument type
     */
    public Type getArgType(int idx) {
        return argTypes[idx];
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!matches) {
            for (SpyContext ctx : ctxs) {
                for (SpyMatcher matcher : ctx.getSpyDefinition().getMatchers()) {
                    if (matcher.matchMethodAnnotation(desc.replace("/", "."))) {
                        matches = true;
                    }
                }
            }
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitCode() {
        mv.visitCode();

        if (!matches) {
            return;
        }

        // TODO we have bug here: if single sdef matches, probes from all sdefs are inserted

        // ON_ENTER probes are inserted here
        for (SpyContext ctx : ctxs) {
            SpyDefinition sdef = ctx.getSpyDefinition();
            if (sdef.getProbes(ON_ENTER).size() > 0 || sdef.getProcessors(ON_ENTER).size() > 0) {
                stackDelta = max(stackDelta, emitProbes(ON_ENTER, ctx));
            }
        }

        mv.visitTryCatchBlock(lTryFrom, lTryTo, lTryHandler, null);
        mv.visitLabel(lTryFrom);
    }


    @Override
    public void visitInsn(int opcode) {
        if (matches && opcode >= IRETURN && opcode <= RETURN) {
            // ON_RETURN probes are inserted here
            if (returnProbe != null) {
                returnProbe.emitFetchRetVal(this, returnType);
            }
            for (int i = ctxs.size()-1; i >= 0; i--) {
                SpyContext ctx = ctxs.get(i);
                SpyDefinition sdef = ctx.getSpyDefinition();
                if (getSubmitFlags(ctx.getSpyDefinition(), ON_ENTER) == SF_NONE ||
                    sdef.getProbes(ON_RETURN).size() > 0 || sdef.getProcessors(ON_RETURN).size() > 0) {
                    stackDelta = max(stackDelta, emitProbes(ON_RETURN, ctx));
                }
            }
        }
        mv.visitInsn(opcode);
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

        if (!matches) {
            mv.visitMaxs(maxStack,  maxLocals);
            return;
        }

        mv.visitLabel(lTryTo);
        mv.visitLabel(lTryHandler);

        if (returnProbe != null) {
            returnProbe.emitFetchRetVal(this, Type.getType(Object.class));
        }

        for (int i = ctxs.size()-1; i >= 0; i--) {
            SpyContext ctx = ctxs.get(i);
            SpyDefinition sdef = ctx.getSpyDefinition();
            if (getSubmitFlags(ctx.getSpyDefinition(), ON_ENTER) == SF_NONE ||
                sdef.getProbes(ON_ERROR).size() > 0 || sdef.getProcessors(ON_ERROR).size() > 0) {
                stackDelta = max(stackDelta, emitProbes(ON_ERROR, ctx));
            }
        }

        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack + stackDelta, max(maxLocals, retValProbeSlot+1));
    }


    /**
     * Returns flag that will be passed by probes of a given sdef on a given stage.
     * This flag is used by submit dispatcher to determine if submit record has to be
     * processed immediately or needs to wait for more data to come.
     *
     * TODO this is propably useless as submitter has reference to spy context, so can determine it by itself
     *
     * @param sdef spy definition
     *
     * @param stage whether it is method entry, return or error handling
     *
     * @return
     */
    public static int getSubmitFlags(SpyDefinition sdef, int stage) {

        if (stage == ON_ENTER) {
            return (sdef.getProbes(ON_RETURN).size() == 0
                    && sdef.getProbes(ON_ERROR).size() == 0)
                    ? SF_IMMEDIATE : SF_NONE;
        } else {
            return (sdef.getProbes(ON_ENTER).size() == 0)
                    ? SF_IMMEDIATE : SF_FLUSH;
        }
    }


    /**
     * Checks if any probe looks for return value or thrown exception object. If so,
     * returnProbe will obtain reference to first probe found and retValProbeSlot will
     * be assigned properly.
     */
    private void checkReturnVals() {

        for (SpyContext ctx : ctxs) {
            for (int stage : new int[] { ON_ERROR, ON_RETURN }) {
                for (SpyProbe spe : ctx.getSpyDefinition().getProbes(stage)) {
                    if (spe instanceof SpyReturnProbe) {
                        returnProbe = spe;
                    }
                }
            }
        }

        if (returnProbe != null) {
            retValProbeSlot = argTypes.length + 1;
        }

    } // checkReturnVals()


    /**
     * Emits bytecode of all probes of given context in given stage (entry point, return, error handling).
     * This method has to be called for each spy context separately. Spy contexts on return/error handling
     * have to be called in reverse order.
     *
     * @param stage stage (ON_ENTER, ON_RETURN, ON_ERROR)
     *
     * @param ctx spy context
     *
     * @return number of JVM stack slots consumed
     */
    private int emitProbes(int stage, SpyContext ctx) {
        SpyDefinition sdef = ctx.getSpyDefinition();
        List<SpyProbe> probeElements = sdef.getProbes(stage);

        int submitFlags = getSubmitFlags(sdef, stage);

        // Put first 3 arguments of MainSubmitter.submit() onto stack
        emitLoadInt(stage);
        emitLoadInt(ctx.getId());
        emitLoadInt(submitFlags);

        int sd = 3;

        // Create an array with fetched data (or push null)
        if (probeElements.size() > 0) {
            emitLoadInt(probeElements.size());
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            for (int i = 0; i < probeElements.size(); i++) {
                SpyProbe element = probeElements.get(i);
                mv.visitInsn(DUP);
                emitLoadInt(i);
                //sd = max(sd, emitProbeElement(stage, 0, probeElements.get(i)) + 6);
                sd = max(sd, probeElements.get(i).emit(this, stage, 0) + 6);
                mv.visitInsn(AASTORE);
            }
        } else {
            mv.visitInsn(ACONST_NULL);
            sd++;
        }

        // Call MainSubmitter.submit()
        mv.visitMethodInsn(INVOKESTATIC, SUBMIT_CLASS, SUBMIT_METHOD, SUBMIT_DESC);

        return sd;
    }


    /**
     * Emits instruction that loads integer constant onto JVM stack
     *
     * @param v constant value
     */
    private void emitLoadInt(int v) {
        if (v >= 0 && v <= 5) {
            mv.visitInsn(ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, v);
        } else {
            mv.visitLdcInsn(new Integer(v));
        }
    }


    /**
     * Injects debug messages into instrumented method. This is sometimes useful to debug
     * instrumentation engine.
     *
     * @param msg message to be injected.
     */
    private void emitDebugPrint(String msg) {
        if (debug) {
            mv.visitFieldInsn(GETSTATIC,
                    "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(msg);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        }
    }

    // TODO consider using LDC instruction to pass spy contexts directly instead of using concurrent context map in dispatcher

}
