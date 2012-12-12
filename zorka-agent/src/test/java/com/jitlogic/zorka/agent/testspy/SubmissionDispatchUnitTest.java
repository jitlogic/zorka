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

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.agent.testspy.support.TestCollector;
import com.jitlogic.zorka.agent.testspy.support.TestSpyTransformer;
import com.jitlogic.zorka.agent.testutil.TestLogger;
import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.spy.DispatchingSubmitter;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.spy.SpySubmitter;

import static com.jitlogic.zorka.spy.SpyConst.*;

import com.jitlogic.zorka.util.ZorkaLogger;
import org.junit.Before;
import org.junit.Test;

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
        SpyDefinition sdef = engine.add(SpyDefinition.instance().onEnter(FETCH_TIME));
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "com.TClass", "tMethod", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_IMMEDIATE, new Object[] { 1L });

        assertEquals(1, collector.size());
    }


    @Test
    public void testSubmitWithBufferAndFlush() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instrument());
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_NONE, new Object[] { 1L });
        assertEquals(0, collector.size());

        submitter.submit(ON_RETURN, ctx.getId(), SF_FLUSH, new Object[] { 2L });
        assertEquals(1, collector.size());
    }


    @Test
    public void testSubmitAndCheckOnCollectBuf() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instance());
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_IMMEDIATE, new Object[] { 1L });

        assertEquals(1, collector.size());
        assertEquals(1, collector.get(0).size(ON_COLLECT));
    }


    @Test
    public void testSubmitAndCheckSubmitBuffer() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instrument());
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        submitter.submit(ON_ENTER, ctx.getId(), SF_NONE, new Object[] { 1L });
        submitter.submit(ON_RETURN, ctx.getId(), SF_FLUSH, new Object[] { 2L });

        assertEquals(1, collector.size());

        SpyRecord sr = collector.get(0);

        assertEquals(0, sr.size(ON_ENTER));
        assertEquals(0, sr.size(ON_RETURN));
        assertEquals(2, sr.size(ON_COLLECT));

        assertEquals(1L, sr.get(ON_COLLECT, 0));
        assertEquals(2L, sr.get(ON_COLLECT, 1));
    }


    @Test
    public void testExtendSpyDefRecordValArray() throws Exception {
        SpyDefinition sdef = engine.add(SpyDefinition.instrument());
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "Class", "method", "()V", 1));

        SpyRecord rec = new SpyRecord(ctx);

        rec.feed(ON_ENTER, new Object[] { 1, 2 });
        rec.put(ON_ENTER, 2, 3);

        assertEquals(3, rec.getVals(ON_ENTER).length);
    }

    // TODO test if SpyRecord marks stages properly

    // TODO test submission stages are marked by DispatchingSubmitter
}
