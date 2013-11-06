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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is main class transformer installed in JVM by Zorka agent (see premain() method).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyClassTransformer implements ClassFileTransformer {

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * All spy defs configured
     */
    private List<SpyDefinition> sdefs = new ArrayList<SpyDefinition>();

    /**
     * SpyContext counter.
     */
    private int nextId = 1;

    /**
     * Map of spy contexts (by ID)
     */
    private Map<Integer, SpyContext> ctxById = new ConcurrentHashMap<Integer, SpyContext>();

    /**
     * Map of spy contexts (by instance)
     */
    private Map<SpyContext, SpyContext> ctxInstances = new HashMap<SpyContext, SpyContext>();

    private ThreadLocal<Boolean> transformLock = new ThreadLocal<Boolean>();

    private ThreadLocal<Set<String>> currentTransformsTL = new ThreadLocal<Set<String>>() {
        @Override
        public Set<String> initialValue() {
            return new HashSet<String>();
        }
    };

    private SymbolRegistry symbolRegistry;

    /**
     * Reference to tracer instance.
     */
    Tracer tracer;

    private MethodCallStatistic tracerLookups, classesProcessed, classesTransformed;

    /**
     * Creates new spy class transformer
     *
     * @param tracer reference to tracer engine object
     */
    public SpyClassTransformer(SymbolRegistry symbolRegistry, Tracer tracer, MethodCallStatistics statistics) {
        this.symbolRegistry = symbolRegistry;
        this.tracer = tracer;

        this.tracerLookups = statistics.getMethodCallStatistic("tracerLookups");
        this.classesProcessed = statistics.getMethodCallStatistic("classesProcessed");
        this.classesTransformed = statistics.getMethodCallStatistic("classesTransformed");
    }

    /**
     * Returns context by its ID
     */
    public SpyContext getContext(int id) {
        return ctxById.get(id);
    }

    /**
     * Looks up for a spy context with the same configuration. If there is one, it will be returned.
     * If there is none, supplied context will be registered and will have an  ID assigned.
     *
     * @param keyCtx sample (possibly unregistered) context
     * @return registered context
     *         <p/>
     *         TODO get rid of this crap, use strings to find already created contexts for a method
     *         TODO BUG one context ID refers only to one sdef, so using multiple sdefs on a single method will result errors (submitting data from all probes only to first one)
     */
    public SpyContext lookup(SpyContext keyCtx) {
        synchronized (this) { // TODO get rid of synchronized, use
            SpyContext ctx = ctxInstances.get(keyCtx);
            if (ctx == null) {
                ctx = keyCtx;
                ctx.setId(nextId++);
                ctxInstances.put(ctx, ctx);
                ctxById.put(ctx.getId(), ctx);
            }
            return ctx;
        }
    }


    /**
     * Adds sdef configuration to transformer. For all subsequent classes pushed through transformer,
     * it will look if this sdef matches and possibly instrument methods according to sdef.
     *
     * @param sdef spy definition
     * @return
     */
    public SpyDefinition add(SpyDefinition sdef) {
        sdefs.add(sdef);
        return sdef;
    }


    /**
     * Resets spy transformer. Removes all added spy definitions.
     * All submissions coming from existing probes will be ignored.
     */
    public void reset() {
        sdefs.clear();
        ctxById.clear();
        ctxInstances.clear();
    }


    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (Boolean.TRUE.equals(transformLock.get())) {
            return classfileBuffer;
        }

        String clazzName = className.replace("/", ".");

        Set<String> currentTransforms = currentTransformsTL.get();
        if (currentTransforms.contains(clazzName)) {
            return classfileBuffer;
        } else {
            currentTransforms.add(clazzName);
        }

        long pt1 = System.nanoTime();

        List<SpyDefinition> found = new ArrayList<SpyDefinition>();

        if (ZorkaLogger.isLogLevel(ZorkaLogger.ZSP_CLASS_TRC)) {
            log.debug(ZorkaLogger.ZSP_CLASS_TRC, "Encountered class: %s", className);
        }

        for (SpyDefinition sdef : sdefs) {

            if (sdef.getMatcherSet().classMatch(clazzName)) {
                found.add(sdef);
            }
        }

        long lt1 = System.nanoTime();
        boolean tracerMatch = tracer.getMatcherSet().classMatch(clazzName);
        long lt2 = System.nanoTime();

        tracerLookups.logCall(lt2 - lt1);

        byte[] buf = classfileBuffer;

        if (found.size() > 0 || tracerMatch) {

            long tt1 = System.nanoTime();

            if (ZorkaLogger.isLogLevel(ZorkaLogger.ZSP_CLASS_TRC)) {
                log.debug(ZorkaLogger.ZSP_CLASS_TRC, "Transforming class: %s (sdefs found: %d; tracer match: %b)",
                        className, found.size(), tracerMatch);
            }

            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor scv = createVisitor(classLoader, clazzName, found, tracer, cw);
            cr.accept(scv, 0);
            buf = cw.toByteArray();

            long tt2 = System.nanoTime();
            classesTransformed.logCall(tt2 - tt1);
        }

        currentTransforms.remove(clazzName);

        long pt2 = System.nanoTime();
        classesProcessed.logCall(pt2 - pt1);

        return buf;
    }

    /**
     * Spawn class visitor for transformed class.
     *
     * @param className class name
     * @param found     spy definitions that match
     * @param cw        output (class writer)
     * @return class visitor for instrumenting this class
     */
    protected ClassVisitor createVisitor(ClassLoader classLoader, String className, List<SpyDefinition> found, Tracer tracer, ClassWriter cw) {
        return new SpyClassVisitor(this, classLoader, symbolRegistry, className, found, tracer, cw);
    }

}
