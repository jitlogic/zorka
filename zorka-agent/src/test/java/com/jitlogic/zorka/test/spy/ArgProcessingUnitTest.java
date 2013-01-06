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

package com.jitlogic.zorka.test.spy;

import com.jitlogic.zorka.test.support.ZorkaFixture;

import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.*;

import static com.jitlogic.zorka.spy.SpyLib.*;

import com.jitlogic.zorka.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ArgProcessingUnitTest extends ZorkaFixture {

    protected SpyContext ctx;
    protected SpyDefinition sdef;
    protected Map<String,Object> record;

    @Before
    public void setUp() {
        zorkaAgent.install("test", this);

        sdef = SpyDefinition.instance();
        ctx = new SpyContext(sdef, "some.Class", "someMethod", "()V", 1);
        record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
    }


    @Test
    public void testTrivialStringFormatArgProcessing() throws Exception {
        SpyProcessor proc = new StringFormatProcessor("E0", "len=${E0.length()}");
        record.put("E0", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        proc.process(record);

        assertEquals("len=4", record.get("E0"));
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
    public void testFilterOutPositiveVal()  throws Exception {
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
        record.put("E0", a); record.put("E1", b);
        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);
        return null != sp.process(record);
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


    private boolean vcmp(Object a, String op, Object v) {
        SpyProcessor sp = ComparatorProcessor.vcmp("E0", op, v);
        record.put("E0", a);
        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_ENTER));
        record.put(".STAGE", SpyLib.ON_ENTER);

        return null != sp.process(record);
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

    @Test
    public void testTdiffProc() {
        SpyProcessor sp = new TimeDiffProcessor("T1", "T2", "T");
        Map<String,Object> in = ZorkaUtil.map("T1", 10L, "T2", 30L);
        Map<String,Object> out = sp.process(in);

        assertEquals(20L, out.get("T"));
    }
}
