/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.jitlogic.zorka.common.ZorkaAgent;
import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.util.ObjectDumper;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import bsh.EvalError;
import bsh.Interpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jitlogic.zorka.core.AgentConfigProps.SCRIPTS_DIR_PROP;
import static com.jitlogic.zorka.core.AgentConfigProps.SCRIPTS_PROP;

/**
 * This is central part of Zorka agent - it processes actual queries and executes BSH scripts.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaBshAgent implements ZorkaAgent, ZorkaService {

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Beanshell interpreter
     */
    private Interpreter interpreter;

    /**
     * Executor for asynchronous processing queries
     */
    private Executor connExecutor;

    private ExecutorService mainExecutor;

    private AgentConfig config;

    private long timeout;

    private boolean initialized;

    private Set<String> loadedScripts = new HashSet<String>();

    private Map<String,String> probeMap = new ConcurrentHashMap<String, String>();

    /**
     * Standard constructor.
     *
     * @param connExecutor connExecutor for asynchronous processing queries
     */
    public ZorkaBshAgent(Executor connExecutor, ExecutorService mainExecutor,
                         long timeout, AgentConfig config) {

        this.interpreter = new Interpreter();

        this.connExecutor = connExecutor;
        this.mainExecutor = mainExecutor;
        this.timeout = timeout;
        this.config = config;

        probeSetup();
    }


    /**
     * Installs object in beanshell namespace. Typically used to install
     * objects as function libraries.
     *
     * @param name name in beanshell namespace
     * @param obj  object
     */
    public void put(String name, Object obj) {
        try {
            interpreter.set(name, obj);
        } catch (EvalError e) {
            log.error("Error adding module '" + name + "' to global namespace", e);
        }
    }

    public Object get(String name) {
        try {
            return interpreter.get(name);
        } catch (EvalError e) {
            return null;
        }
    }


    /**
     * Evaluates BSH query. If error occurs, it returns exception text with stack dump.
     *
     * @param expr query string
     * @return response string
     */
    @Override
    public String query(String expr) {
        try {
            return "" + interpreter.eval(expr); // TODO proper object-to-string conversion
        } catch (EvalError e) {
            log.error("Error evaluating '" + expr + "': ", e);
            return ObjectDumper.errorDump(e);
        }
    }


    /**
     * Evaluates BSH query. If evaluation error occurs, it is thrown out as EvalError.
     *
     * @param expr query string
     * @return evaluation result
     * @throws EvalError
     */
    public Object eval(String expr) throws EvalError {
        return interpreter.eval(expr);
    }


    /**
     * Executes query asynchronously. Result is returned via callback object.
     *
     * @param expr     BSH expression
     * @param callback callback object
     */
    public void exec(String expr, ZorkaCallback callback) {
        log.debug("Processing request BSH expression: " + expr);
        ZorkaBshWorker worker = new ZorkaBshWorker(mainExecutor, timeout, this, expr, callback);
        connExecutor.execute(worker);
    }


    /**
     * Loads and executes beanshell script.
     *
     * @param script path to script
     */
    public synchronized String loadScript(String script) {
        String path = ZorkaUtil.path(config.stringCfg(SCRIPTS_DIR_PROP, null), script);
        Reader rdr = null;
        try {
            if (new File(path).canRead()) {
                log.info("Executing script: " + path);
                interpreter.source(path);
                loadedScripts.add(script);
            } else {
                InputStream is = getClass().getResourceAsStream(
                        "/com/jitlogic/zorka/scripts"+(script.startsWith("/") ? "" : "/")+script);
                if (is != null) {
                    log.info("Executing internal script: " + script);
                    rdr = new InputStreamReader(is);
                    interpreter.eval(rdr, interpreter.getNameSpace(), script);
                    loadedScripts.add(script);
                } else {
                    log.error("Cannot find script: " + script);
                }
            }
            return "OK";
        } catch (Exception e) {
            log.error("Error loading script " + script, e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return "Error: " + e.getMessage();
        } catch (EvalError e) {
            log.error("Error executing script " + script, e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return "Error: " + e.getMessage();
        } finally {
            if (rdr != null) {
                try {
                    rdr.close();
                } catch (IOException e) {
                    log.error("Error closing script " + script, e);
                }
            }
        }
    }

    public synchronized String require(String script) {
        if (!loadedScripts.contains(script)) {
            return loadScript(script);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Skipping already loaded script: " + script);
            }
            return "Already loaded.";
        }
    }

    public Set<String> getLoadedScripts() {
        return loadedScripts;
    }

    public Map<String,String> getProbeMap() {
        return probeMap;
    }

    public void probeSetup() {
        Properties props = config.getProperties();
        for (String s : props.stringPropertyNames()) {
            if (s.startsWith("auto.")) {
                probeMap.put(s.substring(5), config.get(s));
            }
        }
    }


    public synchronized void probe(String className) {
        int nhits = 0;
        for (Map.Entry<String,String> e : probeMap.entrySet()) {
            if (className.startsWith(e.getKey())) {
                require(e.getValue().trim());
                nhits++;
            }
        }

        if (nhits > 0) {
            Iterator<Map.Entry<String, String>> iter = probeMap.entrySet().iterator();
            while (iter.hasNext()) {
                if (loadedScripts.contains(iter.next().getValue())) {
                    iter.remove();
                }
            }
        }
    }


    /**
     * Loads and executes all script in script directory.
     */
    public void loadScripts() {
        String scriptsDir = config.stringCfg(SCRIPTS_DIR_PROP, null);

        if (scriptsDir == null) {
            log.error("Scripts directory not set. Internal error ?!?");
            return;
        }

        List<String> scripts = config.listCfg(SCRIPTS_PROP);

        if (scripts != null) {
            for (String script : scripts) {
                require(script);
            }
        }
    }


    public synchronized void reloadScripts() {
        loadedScripts.clear();
        AgentDiagnostics.clear(AgentDiagnostics.CONFIG_ERRORS);
        loadScripts();
    }


    public void initialize() {
        loadScripts();
        initialized = true;
    }


    public boolean isInitialized() {
        return initialized;
    }

    public void restart() {
        interpreter = new Interpreter();
        put("PROP", AgentConfigProps.PROPS);
    }

    @Override
    public void shutdown() {

    }
}
