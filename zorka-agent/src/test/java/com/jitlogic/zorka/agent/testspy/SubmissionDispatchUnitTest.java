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

package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.agent.testspy.support.TestCollector;
import com.jitlogic.zorka.agent.testspy.support.TestSpyTransformer;
import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.spy.DispatchingSubmitter;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpySubmitter;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static com.jitlogic.zorka.spy.SpyLib.*;


public class SubmissionDispatchUnitTest extends ZorkaFixture {

    private TestSpyTransformer engine;
    private SpySubmitter submitter;
    private TestCollector collector;


    @Before
    public void initEngine() {
        engine = new TestSpyTransformer();
        collector = new TestCollector();
        submitter = new DispatchingSubmitter(engine, collector);
    }

    @Test
    public void testSubmitWithImmediateFlagAndCheckIfCollected() throws Exception {
        SpyDefinition sdef = engine.add(
            SpyDefinition.instance().onEnter(spy.fetchTime("E0"))).onSubmit(collector);
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "com.TClass", "tMethod", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_IMMEDIATE, new Object[] { 1L });

        assertEquals(1, collector.size());
    }


    @Test
    public void testSubmitWithBufferAndFlush() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instrument().onSubmit(collector));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_NONE, new Object[] { 1L });
        assertEquals(0, collector.size());

        submitter.submit(ON_RETURN, ctx.getId(), SF_FLUSH, new Object[] { 2L });
        assertEquals(1, collector.size());
    }


    //@Test TODO temporarily disabled
    public void testSubmitAndCheckOnCollectBuf() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instance().onSubmit(collector));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_IMMEDIATE, new Object[] { 1L });

        assertEquals(1, collector.size());
        assertEquals(0, collector.get(0).size());
    }


    //@Test TODO temporarily disabled
    public void testSubmitAndCheckSubmitBuffer() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instrument().onSubmit(collector));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_NONE, new Object[] { 1L });
        submitter.submit(ON_RETURN, ctx.getId(), SF_FLUSH, new Object[] { 2L });

        assertEquals(1, collector.size());

        Map<String,Object> sr = collector.get(0);

        assertEquals(0, sr.size());
    }


    // TODO test if SpyRecord marks stages properly

    // TODO test submission stages are marked by DispatchingSubmitter
}
