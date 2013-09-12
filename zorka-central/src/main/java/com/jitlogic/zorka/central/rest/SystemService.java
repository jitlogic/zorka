/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.central.rest;

import com.jitlogic.zorka.common.ZorkaAgent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Path("system")
public class SystemService {


    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> systemInfo() {
        List<String> info = new ArrayList<String>();

        // TODO use agent to present these things - it's already there :)

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

        MemoryUsage hmu = mem.getHeapMemoryUsage();

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long ss = uptime % 60, mm = ((uptime - ss) / 60) % 60, hh = ((uptime - mm * 60 - ss) / 3600) % 60,
                dd = ((uptime - hh * 3600 - mm * 60 - ss) / 86400);

        info.add("Uptime: " + String.format("%dd %02d:%02d:%02d", dd, hh, mm, ss));

        info.add("Heap Memory: " + String.format("%dMB/%dMB (%.1f%%)",
                hmu.getUsed() / MB, hmu.getMax() / MB, 100.0 * hmu.getUsed() / hmu.getMax()));

        MemoryUsage nmu = mem.getNonHeapMemoryUsage();

        info.add("Non-Heap Memory: " + String.format("%dMB/%dMB (%.1f%%)",
                nmu.getUsed() / MB, nmu.getMax() / MB, 100.0 * nmu.getUsed() / nmu.getMax()));

        try {
            if (agent != null) {
                info.addAll(Arrays.asList(agent.query("central.info()").split("\n")));
            }
        } catch (Exception e) {
            //log.warn("Call to self-monitoring agent failed.", e);
        }

        return info;
    }

    private static final long MB = 1024 * 1024;

    private ZorkaAgent agent;

    public void setAgent(ZorkaAgent agent) {
        this.agent = agent;
    }

}
