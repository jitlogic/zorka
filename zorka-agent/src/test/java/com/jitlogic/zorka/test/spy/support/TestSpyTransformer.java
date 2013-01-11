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
package com.jitlogic.zorka.test.spy.support;

import com.jitlogic.zorka.spy.SpyClassTransformer;
import com.jitlogic.zorka.spy.SpyClassVisitor;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.List;

public class TestSpyTransformer extends SpyClassTransformer {

    private boolean debug = false;

    public void enableDebug() {
        this.debug = true;
    }

    @Override
    protected ClassVisitor createVisitor(String className, List<SpyDefinition> found, List<SpyMatcher> foundTraceMatchers, ClassWriter cw) {

        if (debug) {
            return new SpyClassVisitor(this, className, found,
                    foundTraceMatchers, new TraceClassVisitor(cw, new PrintWriter(System.out, true)));
        } else {
            return new SpyClassVisitor(this, className, found, foundTraceMatchers, cw);
        }

    }

}
