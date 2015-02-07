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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.core.spy.plugins.*;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import com.jitlogic.zorka.core.spy.SpyLib;
import com.jitlogic.zorka.core.spy.*;

import static com.jitlogic.zorka.core.spy.SpyLib.*;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

public class ArgProcessingUnitTest extends ZorkaFixture {

    protected SpyContext ctx;
    protected SpyDefinition sdef;
    protected Map<String, Object> record;

    @Before
    public void setUp() {
        zorkaAgent.put("test", this);

        sdef = spy.instance("x");
        ctx = new SpyContext(sdef, "some.Class", "someMethod", "()V", 1);
        record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
    }


    @Test
    public void testTrivialStringFormatArgProcessing() throws Exception {
        SpyProcessor proc = spy.format("E0", "len=${E0.length()}");
        record.put("E0", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        proc.process(record);

        assertEquals("len=4", record.get("E0"));
    }

    @Test
    public void testTrivialStringFormatWithLimit() throws Exception {
        SpyProcessor proc = spy.format("S", "1234567890", 5);
        proc.process(record);
        assertEquals("12345", record.get("S"));
    }

    @Test
    public void testTrivialGetterArgProcessing() throws Exception {
        SpyProcessor proc = new GetterProcessor("E0", "E0", "length()");
        record.put("E0", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        proc.process(record);

        assertEquals(4, record.get("E0"));
    }


    @Test
    public void testFilterNullVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor("E0", "[a-z]+");
        record.put("E0", null);

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        assertNull(proc.process(record));
    }


    @Test
    public void testFilterPositiveVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor("E0", "[a-z]+");
        record.put("E0", "abc");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        assertNotNull(proc.process(record));
    }


    @Test
    public void testFilterNegativeVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor("E0", "[a-z]+");
        record.put("E0", "123");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        assertNull(proc.process(record));
    }


    @Test
    public void testFilterOutPositiveVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor("E0", "[a-z]+", true);
        record.put("E0", "abc");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        assertNull(proc.process(record));
    }


    @Test
    public void testFilterOutNegativeVal() throws Exception {
        SpyProcessor proc = new RegexFilterProcessor("E0", "[a-z]+", true);
        record.put("E0", "123");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        assertNotNull(proc.process(record));
    }


    @Test
    public void testMethodCallProcessing() throws Exception {
        SpyProcessor proc = new MethodCallingProcessor("E0", "E0", "length");
        record.put("E0", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        proc.process(record);

        assertEquals(4, record.get("E0"));
    }


    @Test
    public void testMethodCallProcessingWithOneArg() throws Exception {
        SpyProcessor proc = new MethodCallingProcessor("E0", "E0", "substring", 1);
        record.put("E0", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        proc.process(record);

        assertEquals("ja!", record.get("E0"));
    }


    @Test
    public void testOverloadedMethodCallProcessingWithTwoArgs() throws Exception {
        SpyProcessor proc = new MethodCallingProcessor("E0", "E0", "substring", 1, 3);
        record.put("E0", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        proc.process(record);

        assertEquals("ja", record.get("E0"));
    }

    private void checkHRT(String expected, String url) {
        SpyProcessor sp = new RegexFilterProcessor("E0", "E0", "^(https?://[^/]+/[^/]+).*$", "${1}", true);
        record.put("E0", url);

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        sp.process(record);

        assertEquals(expected, record.get("E0"));
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
        SpyProcessor sp = new RegexFilterProcessor("E0", "E0", "^a(.*)", "${0}", "???");

        record.put("E0", "xxx");
        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        sp.process(record);
        assertEquals("???", record.get("E0"));
    }


    private boolean scmp(Object a, String op, Object b) {
        SpyProcessor sp = ComparatorProcessor.scmp("E0", op, "E1");
        record.put("E0", a);
        record.put("E1", b);
        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);
        return null != sp.process(record);
    }


    @Test
    public void testScmpProc() {
        assertEquals(true, scmp(1, EQ, 1));
        assertEquals(false, scmp(1, NE, 1));
        assertEquals(true, scmp(null, EQ, null));
        assertEquals(true, scmp(1L, EQ, 1));
        assertEquals(true, scmp(2, GT, 1));
        assertEquals(true, scmp(1, LT, 2));
        assertEquals(true, scmp(1.0, GE, 1.0));
        assertEquals(false, scmp(1.0011, EQ, 1.0));
        assertEquals(true, scmp(1.0011, GT, 1.0));
        assertEquals(true, scmp(1.0001, EQ, 1.0));

        assertEquals(true, scmp("oja!", EQ, "oja!"));
        assertEquals(false, scmp("oja!", EQ, "oje!"));
    }


    private boolean vcmp(Object a, String op, Object v) {
        SpyProcessor sp = ComparatorProcessor.vcmp("E0", op, v);
        record.put("E0", a);
        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        return null != sp.process(record);
    }


    @Test
    public void testVcmpProc() {
        assertEquals(true, vcmp(1, EQ, 1));
        assertEquals(false, vcmp(1, NE, 1));
        assertEquals(true, vcmp(null, EQ, null));
        assertEquals(true, vcmp(1L, EQ, 1));
        assertEquals(true, vcmp(2, GT, 1));
        assertEquals(true, vcmp(1, LT, 2));
        assertEquals(true, vcmp(1.0, GE, 1.0));
        assertEquals(false, vcmp(1.0011, EQ, 1.0));
        assertEquals(true, vcmp(1.0011, GT, 1.0));
        assertEquals(true, vcmp(1.0001, EQ, 1.0));

        assertEquals(true, vcmp("oja!", EQ, "oja!"));
        assertEquals(false, vcmp("oja!", EQ, "oje!"));
    }


    @Test
    public void testTdiffProc() {
        SpyProcessor sp = new TimeDiffProcessor("T1", "T2", "T");
        Map<String, Object> in = ZorkaUtil.map("T1", 10L, "T2", 30L);
        Map<String, Object> out = sp.process(in);

        assertEquals(20L, out.get("T"));
    }


    @Test
    public void testThreadLocalSet() {
        ThreadLocal tl = new ThreadLocal();
        ThreadLocalProcessor tp = new ThreadLocalProcessor("S", ThreadLocalProcessor.SET, tl);
        Map<String, Object> rec = ZorkaUtil.map("S", "abc");
        assertEquals("processor should pass record without changes", rec, tp.process(rec));
        assertEquals("abc", tl.get());
    }


    @Test
    public void testThreadLocalGet() {
        ThreadLocal tl = new ThreadLocal();
        tl.set("abc");
        ThreadLocalProcessor tp = new ThreadLocalProcessor("S", ThreadLocalProcessor.GET, tl, "length()");
        Map<String, Object> rec = tp.process(new HashMap<String, Object>());
        assertEquals(3, rec.get("S"));
    }


    @Test
    public void testThreadLocalReset() {
        ThreadLocal tl = new ThreadLocal();
        tl.set("abc");
        ThreadLocalProcessor tp = new ThreadLocalProcessor(null, ThreadLocalProcessor.REMOVE, tl);
        tp.process(new HashMap<String, Object>());
        assertNull(tl.get());
    }


    @Test
    public void testFilterDecider() {
        TraceFilterProcessor p = (TraceFilterProcessor) tracer.filterBy("TEST", false,
                util.set(100, 200), util.set(500, 503), util.set(401, 404));
        assertThat(p.decide(100)).isTrue();
        assertThat(p.decide(500)).isFalse();
        assertThat(p.decide(401)).isNull();
        assertThat(p.decide(403)).isFalse();
    }


    @Test
    public void testLogicalAndFilter() {
        SpyProcessor f1 = spy.and(
                spy.vcmp("X", "==", "a"),
                spy.vcmp("Y", "==", "b"));

        record.put("X", "a");
        record.put("Y", "b");
        assertThat(f1.process(record)).isNotNull();

        record.put("X", "b");
        assertThat(f1.process(record)).isNull();

        record.put("Y", "a");
        assertThat(f1.process(record)).isNull();
    }


    @Test
    public void testLogicalOrFilter() {
        SpyProcessor f1 = spy.or(
                spy.vcmp("X", "==", "a"),
                spy.vcmp("Y", "==", "b"));

        record.put("X", "a");
        record.put("Y", "b");
        assertThat(f1.process(record)).isNotNull();

        record.put("X", "b");
        assertThat(f1.process(record)).isNotNull();

        record.put("Y", "a");
        assertThat(f1.process(record)).isNull();
    }


    @Test
    public void testSubchainFilter() {
        SpyProcessor f1 = spy.subchain(
                spy.vcmp("X", "==", "a"),
                spy.vcmp("Y", "==", "b"));

        record.put("X", "a");
        record.put("Y", "b");
        assertThat(f1.process(record)).isNotNull();

        record.put("X", "b");
        assertThat(f1.process(record)).isNotNull();

        record.put("Y", "a");
        assertThat(f1.process(record)).isNotNull();
    }

    @Test
    public void testSimpleCrc32Sum() {
        Map<String, Object> rec = ZorkaUtil.map("VAL", "ABCD");
        SpyProcessor proc = spy.crc32sum("SUM", "VAL");

        proc.process(rec);

        assertEquals("db1720a5", rec.get("SUM"));
    }

    @Test
    public void testSimpleCrc32SumOfCustomType() {
        Map<String, Object> rec = ZorkaUtil.map("VAL", 1234);
        SpyProcessor proc = spy.crc32sum("SUM", "VAL");

        proc.process(rec);

        assertEquals("9be3e0a3", rec.get("SUM"));
    }

    @Test
    public void testLimitedCrc32Sum() {
        Map<String, Object> rec = ZorkaUtil.map("VAL", "ABCD");
        SpyProcessor proc = spy.crc32sum("SUM", "VAL", 4);

        proc.process(rec);

        assertEquals("db17", rec.get("SUM"));
    }

    @Test
    public void testSimpleMd5Sum() {
        Map<String, Object> rec = ZorkaUtil.map("VAL", "ABCD");
        SpyProcessor proc = spy.md5sum("SUM", "VAL");

        proc.process(rec);

        assertEquals("cb08ca4a7bb5f9683c19133a84872ca7", rec.get("SUM"));
    }

    @Test
    public void testSimpleSha1Sum() {
        Map<String, Object> rec = ZorkaUtil.map("VAL", "ABCD");
        SpyProcessor proc = spy.sha1sum("SUM", "VAL");

        proc.process(rec);

        assertEquals("fb2f85c88567f3c8ce9b799c7c54642d0c7b41f6", rec.get("SUM"));
    }
}
