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

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;

import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.spy.processors.*;

import static com.jitlogic.zorka.spy.SpyLib.*;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArgProcessingUnitTest extends ZorkaFixture {

    protected SpyContext ctx;
    protected SpyDefinition sdef;
    protected SpyRecord record;

    @Before
    public void setUp() {
        zorkaAgent.installModule("test", this);

        sdef = SpyDefinition.instance();
        ctx = new SpyContext(sdef, "some.Class", "someMethod", "()V", 1);
        record = new SpyRecord(ctx);
    }


    @Test
    public void testTrivialStringFormatArgProcessing() throws Exception {
        SpyProcessor proc = new StringFormatProcessor(slot(0), "len=${0.length()}");
        record.feed(ON_ENTER, new Object[] { "oja!"});

        proc.process(ON_ENTER, record);

        assertEquals("len=4", record.get(ON_ENTER, 0));
    }


    @Test
    public void testTrivialGetterArgProcessing() throws Exception {
        SpyProcessor proc = new GetterProcessor(slot(0), slot(0), "length()");
        record.feed(ON_ENTER, new Object[] { "oja!"});

        proc.process(ON_ENTER, record);

        assertEquals(4, record.get(ON_ENTER, 0));
    }


    @Test
    public void testFilterNullVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor(slot(0), "[a-z]+");
        record.feed(ON_ENTER, new Object[] { null });

        assertNull(proc.process(ON_ENTER, record));
    }


    @Test
    public void testFilterPositiveVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor(slot(0), "[a-z]+");
        record.feed(ON_ENTER, new Object[] { "abc" });

        assertNotNull(proc.process(ON_ENTER, record));
    }


    @Test
    public void testFilterNegativeVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor(slot(0), "[a-z]+");
        record.feed(ON_ENTER, new Object[] { "123" });

        assertNull(proc.process(ON_ENTER, record));
    }


    @Test
    public void testFilterOutPositiveVal()  throws Exception {
        SpyProcessor proc = new RegexFilterProcessor(slot(0), "[a-z]+", true);
        record.feed(ON_ENTER, new Object[] { "abc" });

        assertNull(proc.process(ON_ENTER, record));
    }


    @Test
    public void testFilterOutNegativeVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor(slot(0), "[a-z]+", true);
        record.feed(ON_ENTER, new Object[] { "123" });

        assertNotNull(proc.process(ON_ENTER, record));
    }


    @Test
    public void testMethodCallProcessing() throws Exception {
        SpyProcessor proc = new MethodCallingProcessor(slot(0), slot(0), "length");
        record.feed(ON_ENTER, new Object[] { "oja!"});

        proc.process(ON_ENTER, record);

        assertEquals(4, record.get(ON_ENTER, 0));
    }


    @Test
    public void testMethodCallProcessingWithOneArg() throws Exception {
        SpyProcessor proc = new MethodCallingProcessor(slot(0), slot(0), "substring", 1);
        record.feed(ON_ENTER, new Object[] { "oja!"});

        proc.process(ON_ENTER, record);

        assertEquals("ja!", record.get(ON_ENTER, 0));
    }


    @Test
    public void testOverloadedMethodCallProcessingWithTwoArgs() throws Exception {
        SpyProcessor proc = new MethodCallingProcessor(slot(0), slot(0), "substring", 1, 3);
        record.feed(ON_ENTER, new Object[] { "oja!"});

        proc.process(ON_ENTER, record);

        assertEquals("ja", record.get(ON_ENTER, 0));
    }

    private void checkHRT(String expected, String url) {
        SpyProcessor sp = new RegexFilterProcessor(slot(0), slot(0), "^(https?://[^/]+/[^/]+).*$", "${1}", true);
        record.feed(ON_ENTER, new Object[] { url });
        sp.process(ON_ENTER,  record);
        assertEquals(expected, record.get(ON_ENTER, 0));
    }


    @Test
    public void testRegexFilterTransformHttpReq() {
        checkHRT("http://jitlogic.com/zorka", "http://jitlogic.com/zorka/some/page.html");
    }


    @Test
    public void testRegexFilterTransformHttpsReq() {
        checkHRT("https://jitlogic.com/zorka", "https://jitlogic.com/zorka/some/page.html");
    }


    @Test
    public void testRegexFilterTransformShortUrl() {
        checkHRT("https://jitlogic.com", "https://jitlogic.com");
    }


    @Test
    public void testFilterNoMatchWithDefVal() {
        SpyProcessor sp = new RegexFilterProcessor(slot(0), slot(0), "^a(.*)", "${0}", "???");

        record.feed(ON_ENTER, new Object[] { "xxx" });
        sp.process(ON_ENTER, record);
        assertEquals("???", record.get(ON_ENTER, 0));
    }


    private boolean scmp(Object a, int op, Object b) {
        SpyProcessor sp = ComparatorProcessor.scmp(slot(0), op, slot(1));
        record.feed(ON_ENTER, new Object[] { a, b });
        return null != sp.process(ON_ENTER, record);
    }


    @Test
    public void testScmpProc() {
        assertEquals(true,  scmp(1, EQ, 1));
        assertEquals(false, scmp(1, NE, 1));
        assertEquals(true,  scmp(null, EQ, null));
        assertEquals(true,  scmp(1L, EQ, 1));
        assertEquals(true,  scmp(2, GT, 1));
        assertEquals(true,  scmp(1, LT, 2));
        assertEquals(true,  scmp(1.0, GE, 1.0));
        assertEquals(false, scmp(1.0011, EQ, 1.0));
        assertEquals(true,  scmp(1.0011, GT, 1.0));
        assertEquals(true,  scmp(1.0001, EQ, 1.0));

        assertEquals(true,  scmp("oja!", EQ, "oja!"));
        assertEquals(false, scmp("oja!", EQ, "oje!"));
    }


    private boolean vcmp(Object a, int op, Object v) {
        SpyProcessor sp = ComparatorProcessor.vcmp(slot(0), op, v);
        record.feed(ON_ENTER, new Object[] { a });
        return null != sp.process(ON_ENTER, record);
    }


    @Test
    public void testVcmpProc() {
        assertEquals(true,  vcmp(1, EQ, 1));
        assertEquals(false, vcmp(1, NE, 1));
        assertEquals(true,  vcmp(null, EQ, null));
        assertEquals(true,  vcmp(1L, EQ, 1));
        assertEquals(true,  vcmp(2, GT, 1));
        assertEquals(true,  vcmp(1, LT, 2));
        assertEquals(true,  vcmp(1.0, GE, 1.0));
        assertEquals(false, vcmp(1.0011, EQ, 1.0));
        assertEquals(true,  vcmp(1.0011, GT, 1.0));
        assertEquals(true,  vcmp(1.0001, EQ, 1.0));

        assertEquals(true,  vcmp("oja!", EQ, "oja!"));
        assertEquals(false, vcmp("oja!", EQ, "oje!"));
    }
}
