/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.integ;

import com.jitlogic.zorka.common.util.JSONReader;
import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.core.integ.zabbix.ActiveCheckQueryItem;

import com.jitlogic.zorka.core.integ.zabbix.ActiveCheckResponse;
import com.jitlogic.zorka.core.test.support.TestUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ZabbixActiveUnitTest {


    private static ActiveCheckQueryItem acqi(String key, int delay, int lastlogsize, int mtime) {
        ActiveCheckQueryItem itm = new ActiveCheckQueryItem();

        itm.setKey(key);
        itm.setDelay(delay);
        itm.setLastlogsize(lastlogsize);
        itm.setMtime(mtime);

        return itm;
    }


    public static ActiveCheckResponse acr(String response, ActiveCheckQueryItem...data) {
        ActiveCheckResponse resp = new ActiveCheckResponse();
        resp.setResponse(response);
        resp.setData(Arrays.asList(data));
        return resp;
    }


    @Test
    public void testSerializeDeserializeActiveCheckQueryItem() {
        ActiveCheckQueryItem itm1 = acqi("test", 1, 2, 3);
        String json = new JSONWriter(false).write(itm1);
        assertEquals("{\"delay\":1,\"key\":\"test\",\"lastlogsize\":2,\"mtime\":3}", json);

        ActiveCheckQueryItem itm2 = new JSONReader().read(json, ActiveCheckQueryItem.class);
        assertEquals(itm1, itm2);
    }


    @Test
    public void testSerializeDeserializeActiveCheckResponse() {
        ActiveCheckResponse res1 = acr("test.response",
            acqi("test1", 11, 12, 13),
            acqi("test2", 21, 22, 23));
        String json = new JSONWriter(false).write(res1);
        assertEquals("{\"data\":[{\"delay\":11,\"key\":\"test1\",\"lastlogsize\":12,\"mtime\":13},{\"delay\":21,\"key\":\"test2\",\"lastlogsize\":22,\"mtime\":23}],\"response\":\"test.response\"}", json);

        ActiveCheckResponse res2 = new JSONReader().read(json, ActiveCheckResponse.class);
        assertEquals(res1, res2);
    }

    @Test
    public void testParseBigActiveCheckResponse() throws Exception {
        String json = new String(TestUtil.readResource("zabbix/testActiveRequest.json"));
        ActiveCheckResponse resp = new JSONReader().read(json, ActiveCheckResponse.class);
        assertNotNull("No response parsed.", resp);
        assertNotNull("No data array in response.", resp.getData());
        assertEquals(31, resp.getData().size());

        for (ActiveCheckQueryItem q : resp.getData()) {
            assertNotNull(q.getKey());
            assertTrue(q.getKey().length() > 0);
            assertTrue(q.getDelay() > 0);
            assertTrue(q.getLastlogsize() == 0);
            assertTrue(q.getMtime() == 0);
        }
    }

}
