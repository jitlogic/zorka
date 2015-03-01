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

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.core.test.spy.support.TestCollector;
import com.jitlogic.zorka.core.test.spy.support.TestSpyTransformer;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.spy.DispatchingSubmitter;
import com.jitlogic.zorka.core.spy.SpyContext;
import com.jitlogic.zorka.core.spy.SpyDefinition;
import com.jitlogic.zorka.core.spy.SpySubmitter;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static com.jitlogic.zorka.core.spy.SpyLib.*;


public class SubmissionDispatchUnitTest extends ZorkaFixture {

    private TestSpyTransformer engine;
    private SpySubmitter submitter;
    private TestCollector collector;


    @Before
    public void initEngine() {
        engine = new TestSpyTransformer(agentInstance.getSymbolRegistry(), agentInstance.getTracer(),
                agentInstance.getRetransformer());
        collector = new TestCollector();
        submitter = new DispatchingSubmitter(engine);
    }

    @Test
    public void testSubmitWithImmediateFlagAndCheckIfCollected() throws Exception {
        SpyDefinition sdef = engine.add(
                spy.instance("x").onEnter(spy.fetchTime("E0"))).onSubmit(collector);
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "com.TClass", "tMethod", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_IMMEDIATE, new Object[]{1L});

        assertEquals(1, collector.size());
    }


    @Test
    public void testSubmitWithBufferAndFlush() throws Exception {
        SpyDefinition sdef = engine.add(spy.instrument("x").onSubmit(collector));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_NONE, new Object[]{1L});
        assertEquals(0, collector.size());

        submitter.submit(ON_RETURN, ctx.getId(), SF_FLUSH, new Object[]{2L});
        assertEquals(1, collector.size());
    }


    @Test
    public void testSubmitAndCheckOnCollectBuf() throws Exception {
        SpyDefinition sdef = engine.add(spy.instance("x").onEnter(collector));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_IMMEDIATE, new Object[]{1L});

        assertEquals(1, collector.size());
        assertEquals(3, collector.get(0).size());
    }


    @Test
    public void testSubmitAndCheckSubmitBuffer() throws Exception {
        SpyDefinition sdef = engine.add(spy.instrument("x").onSubmit(collector));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_NONE, new Object[]{1L});
        submitter.submit(ON_RETURN, ctx.getId(), SF_FLUSH, new Object[]{2L});

        assertEquals(1, collector.size());

        Map<String, Object> sr = collector.get(0);

        assertEquals(6, sr.size());
    }


    // TODO test if SpyRecord marks stages properly

    // TODO test submission stages are marked by DispatchingSubmitter
}
