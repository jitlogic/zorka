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
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class InstrumentationEngine implements ClassFileTransformer {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private List<SpyDefinition> sdefs = new ArrayList<SpyDefinition>();

    private int nextCtxId = 1;
    private Map<Integer, InstrumentationContext> ctxById = new ConcurrentHashMap<Integer, InstrumentationContext>();
    private Map<InstrumentationContext, InstrumentationContext> ctxInstances
                = new HashMap<InstrumentationContext, InstrumentationContext>();


    public InstrumentationContext getContext(long id) {
        return ctxById.get(id);
    }


    public synchronized InstrumentationContext lookupContext(InstrumentationContext keyCtx) {
        InstrumentationContext ctx = ctxInstances.get(keyCtx);
        if (ctx == null) {
            ctx = keyCtx;
            ctx.setId(nextCtxId++);
            ctxInstances.put(ctx, ctx);
            ctxById.put(ctx.getId(), ctx);
        }
        return ctx;
    }


    public void addSpyDef(SpyDefinition sdef) {
        sdefs.add(sdef);
    }


    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        String clazzName = className.replace("/", ".");

        List<SpyDefinition> found = new ArrayList<SpyDefinition>(sdefs.size());

        for (SpyDefinition sdef : sdefs) {
            if (sdef.match(clazzName)) {
                found.add(sdef);
            }
        }

        if (sdefs.size() > 0) {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, 0);
            SpyClassVisitor scv = new SpyClassVisitor(this, className, found, cw);
            cr.accept(scv, 0);
            return cw.toByteArray();
        }

        return classfileBuffer;
    }
}
