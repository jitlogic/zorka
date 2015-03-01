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
    private Map<String, SpyDefinition> sdefs = new LinkedHashMap<String, SpyDefinition>();


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

    private SpyRetransformer retransformer;

    private boolean computeFrames;

    /**
     * Reference to tracer instance.
     */
    Tracer tracer;

    private MethodCallStatistic tracerLookups, classesProcessed, classesTransformed, spyLookups;


    /**
     * Creates new spy class transformer
     *
     * @param tracer reference to tracer engine object
     */
    public SpyClassTransformer(SymbolRegistry symbolRegistry, Tracer tracer, boolean computeFrames,
                               MethodCallStatistics statistics, SpyRetransformer retransformer) {
        this.symbolRegistry = symbolRegistry;
        this.tracer = tracer;
        this.computeFrames = computeFrames;
        this.retransformer = retransformer;

        this.spyLookups = statistics.getMethodCallStatistic("SpyLookups");
        this.tracerLookups = statistics.getMethodCallStatistic("TracerLookups");
        this.classesProcessed = statistics.getMethodCallStatistic("ClassesProcessed");
        this.classesTransformed = statistics.getMethodCallStatistic("ClassesTransformed");

        if (!computeFrames) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Disabling COMPUTE_FRAMES. Remeber to add -XX:-UseSplitVerifier JVM option in JDK7 or -noverify in JDK8.");
        }
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
    public synchronized SpyDefinition add(SpyDefinition sdef) {
        SpyDefinition osdef = sdefs.get(sdef.getName());

        log.info(ZorkaLogger.ZSP_CONFIG, (osdef == null ? "Adding " : "Replacing ")
                + sdef.getName() + " spy definition.");

        boolean shouldRetransform = osdef != null && !osdef.sameProbes(sdef) && !retransformer.isEnabled();

        if (shouldRetransform) {
            log.warn(ZorkaLogger.ZSP_CONFIG, "Cannot overwrite spy definition '" + osdef.getName()
                    + "' because probes have changed and retransform is not possible.");
            return null;
        }

        sdefs.put(sdef.getName(), sdef);

        if (retransformer.isEnabled() && (osdef == null || !osdef.sameProbes(sdef))) {
            retransformer.retransform(osdef != null ? osdef.getMatcherSet() : null, sdef.getMatcherSet(), true);
        } else {
            log.info(ZorkaLogger.ZSP_CONFIG, "Probes didn't change for " + sdef.getName() + ". Retransform not needed.");
        }

        if (osdef != null) {
            for (Map.Entry<SpyContext, SpyContext> e : ctxInstances.entrySet()) {
                SpyContext ctx = e.getValue();
                if (ctx.getSpyDefinition() == osdef) {
                    ctx.setSpyDefinition(sdef);
                }
            }
        }

        return sdef;
    }


    public synchronized void remove(String sdefName) {
        SpyDefinition sdef = sdefs.get(sdefName);
        if (sdef != null) {
            remove(sdef);
        }
    }


    public synchronized void remove(SpyDefinition sdef) {
        if (sdefs.get(sdef.getName()) == sdef) {
            log.info(ZorkaLogger.ZSP_CONFIG, "Removing spy definition: " + sdef.getName());

            sdefs.remove(sdef.getName());

            Set<SpyContext> ctxs = new HashSet<SpyContext>();
            Set<Integer> ids = new HashSet<Integer>();

            for (Map.Entry<Integer, SpyContext> e : ctxById.entrySet()) {
                ids.add(e.getKey());
                ctxs.add(e.getValue());
            }

            for (Integer id : ids) {
                ctxById.remove(id);
            }

            for (SpyContext ctx : ctxs) {
                ctxInstances.remove(ctx);
            }

            if (retransformer.isEnabled()) {
                retransformer.retransform(null, sdef.getMatcherSet(), true);
            }

        } else {
            log.info(ZorkaLogger.ZSP_CONFIG, "Spy definition " + sdef.getName() + " has changed.");
        }
    }


    public SpyDefinition getSdef(String name) {
        return sdefs.get(name);
    }


    public synchronized Set<SpyDefinition> getSdefs() {
        Set<SpyDefinition> ret = new HashSet<SpyDefinition>();
        ret.addAll(sdefs.values());
        return ret;
    }


    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] cbf) throws IllegalClassFormatException {

        if (Boolean.TRUE.equals(transformLock.get())) {
            return cbf;
        }

        if (cbf == null || cbf.length < 128 ||
            cbf[0] != (byte)0xca || cbf[1] != (byte)0xfe ||
            cbf[2] != (byte)0xba || cbf[3] != (byte)0xbe) {
            return cbf;
        }

        String clazzName = className.replace("/", ".");

        Set<String> currentTransforms = currentTransformsTL.get();
        if (currentTransforms.contains(clazzName)) {
            return cbf;
        } else {
            currentTransforms.add(clazzName);
        }

        long pt1 = System.nanoTime();

        List<SpyDefinition> found = new ArrayList<SpyDefinition>();

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_CLASS_TRC)) {
            log.debug(ZorkaLogger.ZSP_CLASS_TRC, "Encountered class: %s", className);
        }

        long st1 = System.nanoTime();
        for (Map.Entry<String, SpyDefinition> e : sdefs.entrySet()) {
            SpyDefinition sdef = e.getValue();
            if (sdef.getMatcherSet().classMatch(clazzName)) {
                found.add(sdef);
            }
        }
        long st2 = System.nanoTime();

        spyLookups.logCall(st2 - st1);

        long lt1 = System.nanoTime();
        boolean tracerMatch = tracer.getMatcherSet().classMatch(clazzName);
        long lt2 = System.nanoTime();

        tracerLookups.logCall(lt2 - lt1);

        byte[] buf = cbf;

        if (found.size() > 0 || tracerMatch) {

            long tt1 = System.nanoTime();

            if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_CLASS_TRC)) {
                log.debug(ZorkaLogger.ZSP_CLASS_TRC, "Transforming class: %s (sdefs found: %d; tracer match: %b)",
                        className, found.size(), tracerMatch);
            }

            boolean doComputeFrames = computeFrames && (cbf[7] > (byte) 0x32);

            ClassReader cr = new ClassReader(cbf);
            ClassWriter cw = new ClassWriter(cr, doComputeFrames ? ClassWriter.COMPUTE_FRAMES : 0);
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
