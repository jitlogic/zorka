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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jitlogic.zorka.spy.SpyLib.SPD_CLASSALL;
import static com.jitlogic.zorka.spy.SpyLib.SPD_CLASSXFORM;

/**
 * This is main class transformer installed in JVM by Zorka agent (see premain() method).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyClassTransformer implements ClassFileTransformer {

    /** Logger */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** All spy defs configured */
    private List<SpyDefinition> sdefs = new ArrayList<SpyDefinition>();

    /** SpyContext counter. */
    private int nextId = 1;

    /** Map of spy contexts (by ID) */
    private Map<Integer, SpyContext> ctxById = new ConcurrentHashMap<Integer, SpyContext>();

    /** Map of spy contexts (by instance) */
    private Map<SpyContext, SpyContext> ctxInstances = new HashMap<SpyContext, SpyContext>();

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
     *
     * @return registered context
     *
     * TODO get rid of this crap, use strings to find already created contexts for a method
     * TODO BUG one context ID refers only to one sdef, so using multiple sdefs on a single method will result errors (submitting data from all probes only to first one)
     */
    public synchronized SpyContext lookup(SpyContext keyCtx) {
        SpyContext ctx = ctxInstances.get(keyCtx);
        if (ctx == null) {
            ctx = keyCtx;
            ctx.setId(nextId++);
            ctxInstances.put(ctx, ctx);
            ctxById.put(ctx.getId(), ctx);
        }
        return ctx;
    }


    /**
     * Adds sdef configuration to transformer. For all subsequent classes pushed through transformer,
     * it will look if this sdef matches and possibly instrument methods according to sdef.
     *
     * @param sdef spy definition
     *
     * @return
     */
    public SpyDefinition add(SpyDefinition sdef) {
        sdefs.add(sdef);
        return sdef;
    }


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        String clazzName = className.replace("/", ".");

        List<SpyDefinition> found = new ArrayList<SpyDefinition>();

        for (SpyDefinition sdef : sdefs) {

            if (SpyInstance.isDebugEnabled(SPD_CLASSALL)) {
                log.debug("Encountered class: " + className);
            }

            if (sdef.match(Arrays.asList(clazzName)) || sdef.hasClassAnnotation()) {

                if (SpyInstance.isDebugEnabled(SPD_CLASSXFORM)) {
                    log.debug("Transforming class: " + className);
                }

                found.add(sdef);
            }
        }

        if (found.size() > 0) {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor scv = createVisitor(clazzName, found, cw);
            cr.accept(scv, 0);
            return cw.toByteArray();
        }

        return classfileBuffer;
    }

    /**
     * Spawn class visitor for transformed class.
     *
     * @param className class name
     *
     * @param found spy definitions that match
     *
     * @param cw output (class writer)
     *
     * @return class visitor for instrumenting this class
     */
    protected ClassVisitor createVisitor(String className, List<SpyDefinition> found, ClassWriter cw) {
        return new SpyClassVisitor(this, className, found, cw);
    }
}
