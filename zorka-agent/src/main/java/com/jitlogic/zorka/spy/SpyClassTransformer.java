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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jitlogic.zorka.spy.SpyLib.SPD_CLASSALL;
import static com.jitlogic.zorka.spy.SpyLib.SPD_CLASSXFORM;

public class SpyClassTransformer implements ClassFileTransformer {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private List<SpyDefinition> sdefs = new ArrayList<SpyDefinition>();

    private int nextId = 1;
    private Map<Integer, SpyContext> ctxById = new ConcurrentHashMap<Integer, SpyContext>();
    private Map<SpyContext, SpyContext> ctxInstances = new HashMap<SpyContext, SpyContext>();

    public SpyContext getContext(int id) {
        return ctxById.get(id);
    }


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


    public SpyDefinition add(SpyDefinition sdef) {
        sdefs.add(sdef);
        return sdef;
    }


    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        String clazzName = className.replace("/", ".");

        List<SpyDefinition> found = new ArrayList<SpyDefinition>();

        for (SpyDefinition sdef : sdefs) {

            if (SpyInstance.isDebugEnabled(SPD_CLASSALL)) {
                log.debug("Encountered class: " + className);
            }

            if (sdef.match(clazzName)) {

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

    protected ClassVisitor createVisitor(String clazzName, List<SpyDefinition> found, ClassWriter cw) {
        return new SpyClassVisitor(this, clazzName, found, cw);
    }
}
