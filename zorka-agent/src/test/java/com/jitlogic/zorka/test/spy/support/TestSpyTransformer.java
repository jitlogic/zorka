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
package com.jitlogic.zorka.test.spy.support;

import com.jitlogic.zorka.agent.spy.SpyClassTransformer;
import com.jitlogic.zorka.agent.spy.SpyClassVisitor;
import com.jitlogic.zorka.agent.spy.SpyDefinition;
import com.jitlogic.zorka.agent.spy.Tracer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.List;

public class TestSpyTransformer extends SpyClassTransformer {

    private boolean debug = false;

    public TestSpyTransformer(Tracer tracer) {
        super(tracer);
    }

    public void enableDebug() {
        this.debug = true;
    }

    @Override
    protected ClassVisitor createVisitor(String className, List<SpyDefinition> found, Tracer tracer, ClassWriter cw) {

        if (debug) {
            return new SpyClassVisitor(this, className, found,
                    tracer, new TraceClassVisitor(cw, new PrintWriter(System.out, true)));
        } else {
            return new SpyClassVisitor(this, className, found, tracer, cw);
        }

    }

}
