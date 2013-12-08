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
package com.jitlogic.zico.core.services;

import com.google.inject.Singleton;
import com.jitlogic.zico.core.TraceTemplateManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.TraceTemplate;
import com.jitlogic.zorka.common.ZorkaAgent;

import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class SystemGwtService {

    private ZicoConfig config;

    private static final long MB = 1024 * 1024;

    private ZorkaAgent agent;

    private TraceTemplateManager templater;

    private UserContext userContext;

    public void setAgent(ZorkaAgent agent) {
        this.agent = agent;
    }

    @Inject
    public SystemGwtService(ZicoConfig config, TraceTemplateManager templater, UserContext userContext) {
        this.config = config; // TODO use annotations instead of handcrafted code
        this.templater = templater;
        this.userContext = userContext;
    }

    public List<TraceTemplate> listTemplates() {
        userContext.checkAdmin();
        return templater.listTemplates();
    }

    public int saveTemplate(TraceTemplate tti) {
        userContext.checkAdmin();
        return templater.save(tti);
    }

    public void removeTemplate(Integer tid) {
        userContext.checkAdmin();
        templater.remove(tid);
    }

    public List<String> systemInfo() {
        List<String> info = new ArrayList<String>();

        // TODO use agent to present these things - it's already there :)
        info.add("Version: " + config.stringCfg("zico.version", "<null>"));

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

        MemoryUsage hmu = mem.getHeapMemoryUsage();

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long ss = uptime % 60, mm = ((uptime - ss) / 60) % 60, hh = ((uptime - mm * 60 - ss) / 3600) % 24,
                dd = ((uptime - hh * 3600 - mm * 60 - ss) / 86400);

        info.add("Uptime: " + String.format("%dd %02d:%02d:%02d", dd, hh, mm, ss));

        info.add("Heap Memory: " + String.format("%dMB/%dMB (%.1f%%)",
                hmu.getUsed() / MB, hmu.getMax() / MB, 100.0 * hmu.getUsed() / hmu.getMax()));

        MemoryUsage nmu = mem.getNonHeapMemoryUsage();

        info.add("Non-Heap Memory: " + String.format("%dMB/%dMB (%.1f%%)",
                nmu.getUsed() / MB, nmu.getMax() / MB, 100.0 * nmu.getUsed() / nmu.getMax()));

        try {
            if (agent != null) {
                info.addAll(Arrays.asList(agent.query("zico.info()").split("\n")));
            }
        } catch (Exception e) {
            //log.warn("Call to self-monitoring agent failed.", e);
        }

        return info;
    }


}
