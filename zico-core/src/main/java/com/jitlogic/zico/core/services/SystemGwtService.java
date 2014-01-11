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
import com.jitlogic.zico.core.*;
import com.jitlogic.zico.core.model.TraceTemplate;
import com.jitlogic.zorka.common.tracedata.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class SystemGwtService {

    private final static Logger log = LoggerFactory.getLogger(TraceDataGwtService.class);

    private ZicoConfig config;

    private static final long MB = 1024 * 1024;

    private TraceTemplateManager templater;

    private UserManager userManager;

    private UserContext userContext;

    private HostStoreManager hsm;


    @Inject
    public SystemGwtService(ZicoConfig config, TraceTemplateManager templater,
                            UserContext userContext, HostStoreManager hsm, UserManager userManager) {
        this.config = config;
        this.templater = templater;
        this.userContext = userContext;
        this.hsm = hsm;
        this.userManager = userManager;
    }


    public List<TraceTemplate> listTemplates() {
        try {
            userContext.checkAdmin();
            return templater.listTemplates();
        } catch (Exception e) {
            // TODO use AOP interceptor to implement logging instead of handcoding them; Guice can do such things
            log.error("Error calling listTemplates()", e);
            throw new ZicoRuntimeException(e.getMessage(), e);
        }
    }


    public int saveTemplate(TraceTemplate tti) {
        try {
            userContext.checkAdmin();
            return templater.save(tti);
        } catch (Exception e) {
            log.error("Error calling saveTemplate()", e);
            throw new ZicoRuntimeException(e.getMessage(), e);
        }
    }


    public void removeTemplate(Integer tid) {
        try {
            userContext.checkAdmin();
            templater.remove(tid);
        } catch (Exception e) {
            log.error("Error calling removeTemplate()", e);
            throw new ZicoRuntimeException(e.getMessage(), e);
        }
    }


    public List<String> systemInfo() {
        List<String> info = new ArrayList<String>();

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

        return info;
    }


    public List<Symbol> getTidMap(String hostName) {
        try {
            Map<Integer,String> tids = hsm.getTids(hostName);

            List<Symbol> rslt = new ArrayList<Symbol>(tids.size());

            for (Map.Entry<Integer,String> e : tids.entrySet()) {
                rslt.add(new Symbol(e.getKey(), e.getValue()));
            }

            return rslt;
        } catch (Exception e) {
            log.error("Error calling listTemplates()", e);
            throw new ZicoRuntimeException(e.getMessage(), e);
        }
    }


    public synchronized void backupConfig() {
        userManager.export();
        templater.export();

        for (HostStore h : hsm.list(null)) {
            h.export();
        }
    }

}
