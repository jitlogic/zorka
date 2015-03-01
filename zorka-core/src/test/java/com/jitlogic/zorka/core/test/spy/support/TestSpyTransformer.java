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
package com.jitlogic.zorka.core.test.spy.support;

import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.List;

public class TestSpyTransformer extends SpyClassTransformer {

    private boolean debug = false;

    private SymbolRegistry sreg;

    public TestSpyTransformer(SymbolRegistry symbolRegistry, Tracer tracer, SpyRetransformer spyRetransformer) {
        super(symbolRegistry, tracer, true, new MethodCallStatistics(), spyRetransformer);
        sreg = symbolRegistry;
    }

    public void enableDebug() {
        this.debug = true;
    }

    @Override
    protected ClassVisitor createVisitor(ClassLoader classLoader, String className, List<SpyDefinition> found, Tracer tracer, ClassWriter cw) {

        if (debug) {
            return new SpyClassVisitor(this, classLoader, sreg, className, found,
                    tracer, new TraceClassVisitor(cw, new PrintWriter(System.out, true)));
        } else {
            return new SpyClassVisitor(this, classLoader, sreg, className, found, tracer, cw);
        }

    }

}
