/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.util.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class InfluxHttpOutput extends ZorkaAsyncThread<String> {

    // TODO refactor into generic HTTP output
    private static ZorkaLog log = ZorkaLogger.getLog(InfluxHttpOutput.class);

    private String url, db;

    public InfluxHttpOutput(String name, Map<String,String> conf) {
        super(name, 4096, 256);
        this.url = conf.get("url");
        this.db = conf.get("db");
        log.info(ZorkaLogger.ZPM_CONFIG, "InfluxDB URL=" + url + ", database=" + db);
    }

    @Override
    protected void process(List<String> lines) {
        try {
            int len = lines.size();
            for (String line : lines) {
                len += line.length();
            }
            StringBuilder sb = new StringBuilder(len);
            for (String line : lines) {
                sb.append(line);
                sb.append('\n');
            }
            String s = sb.toString();
            log.debug(ZorkaLogger.ZPM_RUN_TRACE, "Packet sent: '" + s + "'");
            HttpRequest req = HttpUtil.POST(url+"/write?db="+db, s);
            HttpResponse res = req.go();
            log.debug(ZorkaLogger.ZPM_RUN_DEBUG, "HTTP: " + url + " /write?db=" + db + "  -> " + res.getStatus()
                    + " " + res.getStatusMsg());
            if (res.getStatus() >= 400) {
                throw new ZorkaRuntimeException("Error: " + res.getStatus());
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Error sending performance data", e);
        }
    }
}
