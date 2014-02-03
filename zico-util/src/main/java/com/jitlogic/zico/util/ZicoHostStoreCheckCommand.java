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
package com.jitlogic.zico.util;


import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ZicoHostStoreCheckCommand implements ZicoCommand {

    private final static Logger log = LoggerFactory.getLogger(ZicoHostStoreCheckCommand.class);

    private ZicoConfig config;

    @Override
    public void run(String[] args) throws Exception {

        if (args.length < 2) {
            log.error("Host check command requires at least ZICO home dir and zero or more hostnames.");
            return;
        }

        Properties props = ZorkaConfig.defaultProperties(ZicoConfig.DEFAULT_CONF_PATH);
        props.setProperty("zico.home.dir", args[1]);
        config = new ZicoConfig(props);

        int nthreads = Runtime.getRuntime().availableProcessors() * 2;
        List<String> hosts = new ArrayList<String>(args.length);

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                if (new File(config.getDataDir(), args[i]).isDirectory()) {
                    hosts.add(args[i]);
                } else {
                    log.error("Host " + args[i] + " does not exist. Skipping.");
                }
            }
        } else {
            for (String host : new File(config.getDataDir()).list()) {
                if (new File(config.getDataDir(), host).isDirectory()) {
                    log.info("Adding host to rebuild: " + host);
                    hosts.add(host);
                }
            }
        }

        log.info("Index rebuild will be performed in " + nthreads + " threads.");

        if (hosts.size() == 0) {
            log.warn("No suitable hosts to be migrated. Skipping.");
            return;
        }

        final CountDownLatch toBeProcessed = new CountDownLatch(hosts.size());
        final AtomicLong cpuTime = new AtomicLong(0);
        Executor executor = Executors.newFixedThreadPool(nthreads);

        for (final String host : hosts) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("Starting host " + host);
                        long t1 = System.currentTimeMillis();
                        HostStore hostStore = new HostStore(config, null, host);
                        hostStore.rebuildIndex();
                        long t = System.currentTimeMillis() - t1;
                        cpuTime.addAndGet(t);
                        log.info("Finished host " + host + " (t=" + t + "ms)");
                    } catch (Exception e) {
                        log.error("Error processing host " + host, e);
                    } finally {
                        toBeProcessed.countDown();
                    }
                }
            });
        }

        long t1 = System.currentTimeMillis();
        toBeProcessed.await();
        long t = System.currentTimeMillis()-t1;

        log.info("Migration finished: userTime=" + t + "ms, cpuTime=" + cpuTime.get() + "ms.");
    }

}
