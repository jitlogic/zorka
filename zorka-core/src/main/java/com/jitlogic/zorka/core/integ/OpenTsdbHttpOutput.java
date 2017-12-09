/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OpenTsdbHttpOutput extends ZorkaAsyncThread<String> {

    private static Logger log = LoggerFactory.getLogger(OpenTsdbHttpOutput.class);

    private String url;

    /** Error reporting mode:  */
    private String report;

    public OpenTsdbHttpOutput(String name, Map<String,String> conf) {
        super(name, 512, 1);
        this.url = conf.get("url");
        this.report = conf.get("report");
        if (report == null) {
            report = "";
        } else {
            report = "?" + report;
        }
    }

    @Override
    protected void process(List<String> packets) {
        for (String packet : packets) {
            try {
                HttpRequest req = HttpUtil.POST(url + "/api/put" + report, packet);
                HttpResponse res = req.go();
                log.debug("HTTP: " + url + "/api/put" + report + " -> " + res.getStatus() + " " + res.getStatusMsg());
                if (res.getStatus() >= 400) {
                    log.warn("OpenTSDB returned error: " + res.getStatus()
                            + " (" + res.getStatusMsg() + ")");
                    if (log.isDebugEnabled()) {
                        log.debug("OpenTSDB request: " + req.getBodyAsString());
                        log.debug("OpenRSDB reply: " +  res.getBodyAsString());
                    }
                }
            } catch (IOException e) {
                log.error("Error sending performance data", e);
            }
        }
    }
}
