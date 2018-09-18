/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.stats.ValGetter;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.integ.zabbix.ZabbixActiveAgent;
import com.jitlogic.zorka.core.integ.zabbix.ZabbixAgent;
import com.jitlogic.zorka.core.integ.zabbix.ZabbixLib;
import com.jitlogic.zorka.core.integ.zabbix.ZabbixQueryTranslator;
import com.jitlogic.zorka.core.mbeans.AttrGetter;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.core.perfmon.PerfMonLib;
import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.integ.*;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.normproc.NormLib;
import com.jitlogic.zorka.core.spy.lt.LTracer;
import com.jitlogic.zorka.core.spy.lt.LTracerLib;
import com.jitlogic.zorka.core.spy.st.STraceBufManager;
import com.jitlogic.zorka.core.spy.st.STracer;
import com.jitlogic.zorka.core.spy.st.STracerLib;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;
import com.jitlogic.zorka.core.util.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;
import java.util.concurrent.*;

/**
 * This method binds together all components to create fuunctional Zorka agent. It is responsible for
 * initializing all subsystems (according to property values in zorka configuration file) and starting
 * all service threads if necessary. It retains references to created components and maintains reference
 * to MBean server registry and its own instance singletons.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AgentInstance implements ZorkaService {

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * MBean server registry
     */
    private MBeanServerRegistry mBeanServerRegistry;

    /**
     * Handles accepted connections.
     */
    private Executor connExecutor;

    /**
     * Handles BSH requests (called from connection handlers).
     */
    private ExecutorService mainExecutor;

    /**
     * Handles scheduled tasks
     */
    private ScheduledExecutorService scheduledExecutor;
    
    /**
     * Main zorka agent object - one that executes actual requests
     */
    private ZorkaBshAgent zorkaAgent;

    /**
     * Reference to zabbix active agent object - one that handles zabbix active requests and passes them to BSH agent
     */
    private ZabbixActiveAgent zabbixActiveAgent;
    
    /**
     * Reference to zabbix agent object - one that handles zabbix requests and passes them to BSH agent
     */
    private ZabbixAgent zabbixAgent;

    /**
     * Reference to zorka library - basic agent functions available to zorka scripts as 'zorka.*'
     */
    private ZorkaLib zorkaLib;

    /**
     * Reference to zabbix library - available to zorka scripts as 'zabbix.*' functions
     */
    private ZabbixLib zabbixLib;

    /**
     * Reference to nagios agents - one that handles nagios NRPE requests and passes them to BSH agent
     */
    private NagiosAgent nagiosAgent;

    /**
     * Reference to nagios library - available to zorka scripts as 'nagios.*' functions
     */
    private NagiosLib nagiosLib;

    /**
     * Reference to spy library - available to zorka scripts as 'spy.*' functions
     */
    private SpyLib spyLib;

    /**
     * Tracer library
     */
    private TracerLib tracerLib;

    /**
     * Reference to syslog library - available to zorka scripts as 'syslog.*' functions
     */
    private SyslogLib syslogLib;

    private UtilLib utilLib;

    /**
     * Reference to SNMP library - available to zorka scripts as 'snmp.*' functions
     */
    private SnmpLib snmpLib;

    /**
     * Reference to normalizers library - available to zorka scripts as 'normalizers.*' function
     */
    private NormLib normLib;

    /**
     * Reference to ranking and metrics processing library.
     */
    private PerfMonLib perfMonLib;

    private MethodCallStatistics stats = new MethodCallStatistics();

    private MetricsRegistry metricsRegistry;

    private SymbolRegistry symbolRegistry;

    private ZtxMatcherSet tracerMatcherSet;

    private TracerTuner tracerTuner;

    private Tracer tracer;

    private SpyClassTransformer classTransformer;

    private DispatchingSubmitter submitter;

    private AgentConfig config;

    private ZabbixQueryTranslator translator;

    private SpyRetransformer retransformer;

    private STraceBufManager bufManager;

    public AgentInstance(AgentConfig config, SpyRetransformer retransformer) {
        this.config = config;
        this.retransformer = retransformer;
    }


    /**
     * Starts agent. Real startup sequence is performed here.
     */
    public void start() {

        config.initLoggers();

        initBshLibs();

        zorkaAgent.initialize();

        if (config.boolCfg("zorka.diagnostics", true)) {
            createZorkaDiagMBean();
        }
    }


    private void initBshLibs() {

        ZorkaBshAgent bsh = getZorkaAgent();

        bsh.put("zorka", getZorkaLib());

        bsh.put("util", getUtilLib());

        if (config.boolCfg("spy", true)) {
            log.info("Enabling Zorka SPY");
            bsh.put("spy", getSpyLib());
            bsh.put("tracer", getTracerLib());
        }

        bsh.put("perfmon", getPerfMonLib());
        
        if (config.boolCfg("syslog", true)) {
            log.info("Enabling Syslog subsystem ....");
            this.zorkaAgent.put("syslog", getSyslogLib());
        }

        if (config.boolCfg("snmp", true)) {
            log.info("Enabling SNMP subsystem ...");
            this.zorkaAgent.put("snmp", getSnmpLib());
        }

        bsh.put("normalizers", getNormLib());

    }


    public void createZorkaDiagMBean() {
        String mbeanName = config.get("zorka.diagnostics.mbean").trim();

        MBeanServerRegistry registry = getMBeanServerRegistry();

        registry.getOrRegister("java", mbeanName, "Version",
                config.stringCfg("zorka.version", "unknown"), "Agent Diagnostics");


        for (int i = 0; i < AgentDiagnostics.numCounters(); i++) {
            final int counter = i;
            registry.getOrRegister("java", mbeanName, AgentDiagnostics.getName(counter),
                    new ValGetter() {
                        @Override
                        public Object get() {
                            return AgentDiagnostics.get(counter);
                        }
                    });

        }

        registry.getOrRegister("java", mbeanName, "SymbolsCreated",
                new AttrGetter(getSymbolRegistry(), "size()"));

        registry.getOrRegister("java", mbeanName, "stats", stats);
    }


    public AgentConfig getConfig() {
        return config;
    }


    public synchronized QueryTranslator getTranslator() {
        if (translator == null) {
            translator = new ZabbixQueryTranslator();
        }
        return translator;
    }


    public synchronized ZtxMatcherSet getTracerMatcherSet() {
        if (tracerMatcherSet == null) {

            File ztxDir = config.hasCfg("tracer.tuner.dir")
                    ? new File(config.stringCfg("tracer.tuner.dir", null))
                    : new File(config.getHomeDir(), "tuner");

            File ztxLog = config.hasCfg("tracer.tuner.xlog")
                    ? new File(config.stringCfg("tracer.tuner.xlog", null))
                    : new File(ztxDir, "_log.ztx");

            tracerMatcherSet = new ZtxMatcherSet(ztxDir, ztxLog, getSymbolRegistry(), false);
        }
        return tracerMatcherSet;
    }


    public synchronized MetricsRegistry getMetricsRegistry() {
        if (metricsRegistry == null) {
            metricsRegistry = new MetricsRegistry();
        }
        return metricsRegistry;
    }


    public synchronized SymbolRegistry getSymbolRegistry() {
        if (symbolRegistry == null) {
            symbolRegistry = new SymbolRegistry();
        }
        return symbolRegistry;
    }


    private synchronized Executor getConnExecutor() {
        if (connExecutor == null) {
            int rt = config.intCfg("zorka.req.threads", 16);
            connExecutor = new ThreadPoolExecutor(rt, rt, 1000, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(config.intCfg("zorka.req.queue", 256)),
                    new DaemonThreadFactory("ZORKA-conn-pool"));
        }
        return connExecutor;
    }


    private synchronized ExecutorService getMainExecutor() {
        if (mainExecutor == null) {
            int rt = config.intCfg("zorka.req.threads", 16);
            mainExecutor = new ThreadPoolExecutor(rt, rt, 1000, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(config.intCfg("zorka.req.queue", 256)),
                    new DaemonThreadFactory("ZORKA-main-pool"));
        }
        return mainExecutor;
    }

    private synchronized ScheduledExecutorService getScheduledExecutor() {
        if (scheduledExecutor == null) {
            int rt = config.intCfg("zorka.req.threads", 8);
            scheduledExecutor = Executors.newScheduledThreadPool(rt, new DaemonThreadFactory("ZORKA-thread-pool"));
        }
        return scheduledExecutor;
    }


    public synchronized STraceBufManager getBufManager() {
        if (bufManager == null) {
            bufManager = new STraceBufManager(
                    getConfig().intCfg("tracer.chunk.size", 65536),
                    getConfig().intCfg("tracer.chunk.max", 16));
        }
        return bufManager;
    }

    public synchronized TracerTuner getTracerTuner() {
        if (tracerTuner == null) {
            if (config.boolCfg("tracer.tuner", false)) {
                log.info("Enabling automatic tracer tuner.");
                tracerTuner = new TracerTuner(getConfig(), getSymbolRegistry(), getRetransformer());
                tracerTuner.start();
            }
        }
        return tracerTuner;
    }

    public synchronized Tracer getTracer() {
        if (tracer == null) {
            if ("streaming".equals(config.stringCfg("tracer.type", "local"))) {
                log.info("STREAMING tracer selected.");
                tracer = new STracer(config, getTracerMatcherSet(), getSymbolRegistry(), getTracerTuner(), getBufManager());
                MainSubmitter.setTracer(tracer);
            } else {
                log.info("LOCAL tracer selected.");
                tracer = new LTracer(getTracerMatcherSet(), getSymbolRegistry(), getTracerTuner());
                MainSubmitter.setTracer(tracer);
            }
        }
        return tracer;
    }


    public synchronized SpyRetransformer getRetransformer() {
        return retransformer;
    }


    public synchronized SpyClassTransformer getClassTransformer() {
        if (classTransformer == null) {
            classTransformer = new SpyClassTransformer(
                getSymbolRegistry(),
                getTracer(),
                getZorkaAgent(),
                getConfig(),
                stats, getRetransformer());
        }
        return classTransformer;
    }

    public synchronized DispatchingSubmitter getSubmitter() {
        if (submitter == null) {
            submitter = new DispatchingSubmitter(getClassTransformer());
        }
        return submitter;
    }

    /**
     * Returns reference to BSH agent.
     *
     * @return instance of Zorka BSH agent
     */
    public synchronized ZorkaBshAgent getZorkaAgent() {
        if (zorkaAgent == null) {
            long timeout = config.longCfg("zorka.req.timeout", 5000L);
            zorkaAgent = new ZorkaBshAgent(getConnExecutor(), getMainExecutor(), timeout, config);
        }
        return zorkaAgent;
    }


    public synchronized NagiosAgent getNagiosAgent() {
        if (nagiosAgent == null) {
            nagiosAgent = new NagiosAgent(config, getZorkaAgent(), getTranslator());
        }
        return nagiosAgent;
    }

    public synchronized ZabbixActiveAgent getZabbixActiveAgent() {
        if (zabbixActiveAgent == null) {
            zabbixActiveAgent = new ZabbixActiveAgent(config, getZorkaAgent(), getTranslator(), getScheduledExecutor());
        }
        return zabbixActiveAgent;
    }

    public synchronized ZabbixAgent getZabbixAgent() {
        if (zabbixAgent == null) {
            zabbixAgent = new ZabbixAgent(config, getZorkaAgent(), getTranslator());
        }
        return zabbixAgent;
    }


    public synchronized ZorkaLib getZorkaLib() {
        if (zorkaLib == null) {
            zorkaLib = new ZorkaLib(this, getTranslator());
        }
        return zorkaLib;
    }


    public synchronized ZabbixLib getZabbixLib() {
        if (zabbixLib == null) {
            zabbixLib = new ZabbixLib(getMBeanServerRegistry(), config);
        }
        return zabbixLib;
    }


    public synchronized UtilLib getUtilLib() {
        if (utilLib == null) {
            utilLib = new UtilLib();
        }
        return utilLib;
    }


    /**
     * Returns reference to syslog library.
     *
     * @return instance of syslog library
     */
    public synchronized SyslogLib getSyslogLib() {

        if (syslogLib == null) {
            syslogLib = new SyslogLib(config);
        }

        return syslogLib;
    }


    /**
     * Returns reference to Spy library
     *
     * @return instance of spy library
     */
    public synchronized SpyLib getSpyLib() {

        if (spyLib == null) {
            spyLib = new SpyLib(getClassTransformer(), getMBeanServerRegistry());
        }

        return spyLib;
    }


    /**
     * Returns reference to tracer library.
     *
     * @return instance of tracer library
     */
    public synchronized TracerLib getTracerLib() {

        if (tracerLib == null) {
            if (tracer instanceof LTracer) {
                tracerLib = new LTracerLib(getSymbolRegistry(), getMetricsRegistry(), getTracer(), config);
            } else {
                tracerLib = new STracerLib(getSymbolRegistry(), getMetricsRegistry(), getTracer(), config);
            }
        }

        return tracerLib;
    }


    public synchronized NagiosLib getNagiosLib() {

        if (nagiosLib == null) {
            nagiosLib = new NagiosLib(getMBeanServerRegistry());
        }

        return nagiosLib;
    }


    public synchronized NormLib getNormLib() {

        if (normLib == null) {
            normLib = new NormLib();
        }

        return normLib;
    }


    /**
     * Returns reference to SNMP library
     *
     * @return instance of snmp library
     */
    public synchronized SnmpLib getSnmpLib() {

        if (snmpLib == null) {
            snmpLib = new SnmpLib(config);
        }

        return snmpLib;
    }


    /**
     * Returns reference to rank processing & metrics library
     *
     * @return instance of perfmon library
     */
    public synchronized PerfMonLib getPerfMonLib() {

        if (perfMonLib == null) {
            perfMonLib = new PerfMonLib(getSymbolRegistry(), getMetricsRegistry(), getTracer(), getMBeanServerRegistry());
        }

        return perfMonLib;
    }


    /**
     * Returns reference to mbean server registry.
     *
     * @return mbean server registry reference of null (if not yet initialized)
     */
    public MBeanServerRegistry getMBeanServerRegistry() {

        if (mBeanServerRegistry == null) {
            mBeanServerRegistry = new MBeanServerRegistry();
        }

        return mBeanServerRegistry;
    }


    @Override
    public void shutdown() {

        log.info("Shutting down agent ...");

        tracer.clearMatchers();
        tracer.shutdown();

        if (zorkaLib != null) {
            zorkaLib.shutdown();
        }

        if (snmpLib != null) {
            snmpLib.shutdown();
        }

        if (zabbixLib != null) {
            zabbixLib.shutdown();
        }

        if (syslogLib != null) {
            syslogLib.shutdown();
        }

        if (zabbixAgent != null) {
            zabbixAgent.shutdown();
        }
        
        if (zabbixActiveAgent != null) {
        	zabbixActiveAgent.shutdown();
        }

        if (nagiosAgent != null) {
            nagiosAgent.shutdown();
        }
    }


    public void restart() {
        log.info("Reloading agent configuration...");
        config.reload();
        // TODO reconfigure logger here
        config.initLoggers();
        log.info("Agent configuration reloaded ...");

        if (config.boolCfg("zabbix", true)) {
            getZabbixAgent().restart();
        }

        if (config.boolCfg("zabbix.active", false)) {
            getZabbixActiveAgent().restart();
        }
        
        if (config.boolCfg("nagios", true)) {
            getNagiosAgent().restart();
        }

        getZorkaAgent().restart();
        initBshLibs();
        getZorkaAgent().reloadScripts();
        long l = AgentDiagnostics.get(AgentDiagnostics.CONFIG_ERRORS);
        log.info("Agent configuration scripts executed (" + l + " errors).");
        log.info("Number of matchers in tracer configuration: "
                + tracer.getMatcherSet().getMatchers().size());


    }

    public void reload() {
        PatternMatcherSet oldSet = getTracer().getMatcherSet().getPatternMatcherSet();
        SpyClassTransformer transformer = getClassTransformer();
        Set<SpyDefinition> oldSdefs = transformer.getSdefs();

        // Restart agent ...
        shutdown();
        ZorkaUtil.sleep(1000);
        restart();

        long l = AgentDiagnostics.get(AgentDiagnostics.CONFIG_ERRORS);

        if (l != 0) {
            log.info("Error loading configuration scripts. ");
            getTracer().getMatcherSet().setPatternMatcherSet(oldSet);
        }

        ZorkaBshAgent bsh = getZorkaAgent();

        // Check for scripts to load automatically
        if (getConfig().boolCfg("scripts.auto", false)) {
            if (log.isDebugEnabled()) {
                log.debug("Loaded scripts (before): " + bsh.getLoadedScripts());
            }
            Class[] allLoadedClasses = getRetransformer().getAllLoadedClasses();
            log.info("Probing for BSH scripts (" + allLoadedClasses.length + " classes to be probed).");
            bsh.probeSetup();
            for (Class c : allLoadedClasses) {
                if (log.isTraceEnabled()) {
                    log.trace("Probing class: " + c.getName());
                }
                bsh.probe(c.getName());
            }
        }

        PatternMatcherSet newSet = getTracer().getMatcherSet().getPatternMatcherSet();
        log.info("Reinstrumenting classes for tracer ...");
        getRetransformer().retransform(oldSet, newSet, false);
        log.info("Checking for old sdefs to be removed...");
        int removed = 0;
        for (SpyDefinition sdef : oldSdefs) {
            if (sdef == transformer.getSdef(sdef.getName())) {
                transformer.remove(sdef);
                removed++;
            }
        }
        log.info("Number of sdefs removed: " + removed);

    }
}
