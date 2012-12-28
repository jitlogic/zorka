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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.agent.testspy.support.TestCollector;
import com.jitlogic.zorka.agent.testspy.support.TestSpyTransformer;
import com.jitlogic.zorka.agent.testutil.ZorkaFixture;

import com.jitlogic.zorka.spy.*;

import static com.jitlogic.zorka.spy.SpyLib.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollectionDispatchUnitTest extends ZorkaFixture {

    private TestSpyTransformer engine;
    private SpyProcessor collector;

    @Before
    public void setUp() {
        engine = new TestSpyTransformer();
        collector = new DispatchingCollector();
    }

    @Test
    public void testSimpleTwoWayDispatch() {

        TestCollector col1 = new TestCollector();
        TestCollector col2 = new TestCollector();

        SpyDefinition sdef = SpyDefinition.instance().onEnter(spy.fetchTime("E0")).onSubmit(col1, col2);
        SpyContext ctx = engine.lookup(new SpyContext(sdef, "TClass", "method", "()V", 1));

        SpyRecord sr = new SpyRecord(ctx);

        collector.process(SpyLib.ON_SUBMIT, sr);

        assertEquals(1, col1.size());
        assertEquals(1, col2.size());
    }


}
