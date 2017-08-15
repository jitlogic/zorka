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
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.IOException;
import java.util.List;

public class InfluxHttpOutput extends ZorkaAsyncThread<String> {

    private String url;

    public InfluxHttpOutput(String name, String url) {
        super(name, 1024, 256);
        this.url = url;
    }

    @Override
    protected void process(List<String> lines) {
        try {
            HttpRequest req = HttpUtil.POST(url, ZorkaUtil.join("\n", lines));
            req.go();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Error sending performance data", e);
        }
    }
}
