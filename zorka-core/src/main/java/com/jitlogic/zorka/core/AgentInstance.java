/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
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
import com.jitlogic.zorka.core.util.DaemonThreadFactory;

import java.util.Properties;
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
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

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

    /**
     * Agent configuration properties
     */
    private Properties props;                // TODO get rid of this, access configuration via ZorkaConfig methods

    private Tracer tracer;

    private SpyClassTransformer classTransformer;

    private DispatchingSubmitter submitter;

    private AgentConfig config;

    private ZabbixQueryTranslator translator;

    private SpyRetransformer retransformer;

    public AgentInstance(AgentConfig config, SpyRetransformer retransformer) {
        this.config = config;
        props = config.getProperties();
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

        getZorkaAgent().put("zorka", getZorkaLib());

        getZorkaAgent().put("util", getUtilLib());

        if (config.boolCfg("spy", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Zorka SPY");
            getZorkaAgent().put("spy", getSpyLib());
            getZorkaAgent().put("tracer", getTracerLib());
        }

        getZorkaAgent().put("perfmon", getPerfMonLib());

        if (config.boolCfg("zabbix.active", false)) {
        	log.info(ZorkaLogger.ZAG_CONFIG, "Enabling ZABBIX Active Agent subsystem ...");
        	getZabbixActiveAgent().start();
            zorkaAgent.put("zabbix.active", getZabbixLib());
        }
        
        if (config.boolCfg("zabbix", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling ZABBIX subsystem ...");
            getZabbixAgent().start();
            zorkaAgent.put("zabbix", getZabbixLib());
        }

        if (config.boolCfg("syslog", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Syslog subsystem ....");
            zorkaAgent.put("syslog", getSyslogLib());
        }

        if (config.boolCfg("snmp", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling SNMP subsystem ...");
            zorkaAgent.put("snmp", getSnmpLib());
        }

        if (config.boolCfg("nagios", false)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Nagios support ...");
            getNagiosAgent().start();
            zorkaAgent.put("nagios", getNagiosLib());
        }

        getZorkaAgent().put("normalizers", getNormLib());

    }


    public void createZorkaDiagMBean() {
        String mbeanName = props.getProperty("zorka.diagnostics.mbean").trim();

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


    private SpyMatcherSet tracerMatcherSet;

    public synchronized SpyMatcherSet getTracerMatcherSet() {
        if (tracerMatcherSet == null) {
            tracerMatcherSet = new SpyMatcherSet();
        }
        return tracerMatcherSet;
    }


    private MetricsRegistry metricsRegistry;

    public synchronized MetricsRegistry getMetricsRegistry() {
        if (metricsRegistry == null) {
            metricsRegistry = new MetricsRegistry();
        }
        return metricsRegistry;
    }


    private SymbolRegistry symbolRegistry;

    public synchronized SymbolRegistry getSymbolRegistry() {
        if (symbolRegistry == null) {
            symbolRegistry = new SymbolRegistry();
        }
        return symbolRegistry;
    }


    private synchronized Executor getConnExecutor() {
        if (connExecutor == null) {
            int rt = config.intCfg("zorka.req.threads", 8);
            connExecutor = new ThreadPoolExecutor(rt, rt, 1000, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(config.intCfg("zorka.req.queue", 64)),
                    new DaemonThreadFactory("ZORKA-conn-pool"));
        }
        return connExecutor;
    }


    private synchronized ExecutorService getMainExecutor() {
        if (mainExecutor == null) {
            int rt = config.intCfg("zorka.req.threads", 8);
            mainExecutor = new ThreadPoolExecutor(rt, rt, 1000, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(config.intCfg("zorka.req.queue", 64)),
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

    public synchronized Tracer getTracer() {
        if (tracer == null) {
            tracer = new Tracer(getTracerMatcherSet(), getSymbolRegistry());
            MainSubmitter.setTracer(getTracer());
        }
        return tracer;
    }


    public synchronized SpyRetransformer getRetransformer() {
        return retransformer;
    }


    public synchronized SpyClassTransformer getClassTransformer() {
        if (classTransformer == null) {
            classTransformer = new SpyClassTransformer(getSymbolRegistry(), getTracer(),
                getConfig().boolCfg("zorka.spy.compute.frames", true), stats, getRetransformer());
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
            tracerLib = new TracerLib(getSymbolRegistry(), getMetricsRegistry(), getTracer(), config);
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

        log.info(ZorkaLogger.ZAG_CONFIG, "Shutting down agent ...");

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
        log.info(ZorkaLogger.ZAG_CONFIG, "Reloading agent configuration...");
        config.reload();
        ZorkaLogger.getLogger().shutdown();
        config.initLoggers();
        log.info(ZorkaLogger.ZAG_CONFIG, "Agent configuration reloaded ...");

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
        log.info(ZorkaLogger.ZAG_CONFIG, "Agent configuration scripts executed (" + l + " errors).");
        log.info(ZorkaLogger.ZAG_CONFIG, "Number of matchers in tracer configuration: "
                + tracer.getMatcherSet().getMatchers().size());


    }

    public void reload() {
        SpyMatcherSet oldSet = getTracer().getMatcherSet();
        SpyClassTransformer classTransformer = getClassTransformer();
        Set<SpyDefinition> oldSdefs = classTransformer.getSdefs();
        shutdown();
        ZorkaUtil.sleep(1000);
        restart();
        long l = AgentDiagnostics.get(AgentDiagnostics.CONFIG_ERRORS);
        if (l == 0) {
            SpyMatcherSet newSet = getTracer().getMatcherSet();
            log.info(ZorkaLogger.ZAG_CONFIG, "Reinstrumenting classes for tracer ...");
            getRetransformer().retransform(oldSet, newSet, false);
            log.info(ZorkaLogger.ZAG_CONFIG, "Checking for old sdefs to be removed...");
            int removed = 0;
            for (SpyDefinition sdef : oldSdefs) {
                if (sdef == classTransformer.getSdef(sdef.getName())) {
                    classTransformer.remove(sdef);
                    removed++;
                }
            }
            log.info(ZorkaLogger.ZAG_CONFIG, "Number of sdefs removed: " + removed);
        } else {
            log.info(ZorkaLogger.ZAG_CONFIG,
                    "Reinstrumentating classes for tracer skipped due to configuration errors. Fix config scripts and try again.");
            getTracer().setMatcherSet(oldSet);
        }

    }
}
