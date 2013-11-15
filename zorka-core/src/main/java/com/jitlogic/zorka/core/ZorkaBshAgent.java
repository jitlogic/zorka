/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.jitlogic.zorka.common.ZorkaAgent;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.util.ObjectDumper;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.util.ZorkaLog;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * This is central part of Zorka agent - it processes actual queries and executes BSH scripts.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaBshAgent implements ZorkaAgent {

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Beanshell interpreter
     */
    private Interpreter interpreter;

    /**
     * Zorka standard library
     */
    private ZorkaLib zorkaLib;

    /**
     * Executor for asynchronous processing queries
     */
    private Executor connExecutor;

    private ExecutorService mainExecutor;

    private AgentConfig config;

    private long timeout;

    private boolean initialized;

    private Set<String> loadedScripts = new ConcurrentSkipListSet<String>();

    /**
     * Standard constructor.
     *
     * @param connExecutor connExecutor for asynchronous processing queries
     */
    public ZorkaBshAgent(Executor connExecutor, ExecutorService mainExecutor,
                         long timeout, MBeanServerRegistry mbsRegistry,
                         AgentConfig config, QueryTranslator translator) {

        this.interpreter = new Interpreter();

        this.connExecutor = connExecutor;
        this.mainExecutor = mainExecutor;
        this.timeout = timeout;
        this.config = config;


        zorkaLib = new ZorkaLib(this, mbsRegistry, config, translator);
        put("zorka", zorkaLib);
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
            log.error(ZorkaLogger.ZAG_ERRORS, "Error adding module '" + name + "' to global namespace", e);
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
            log.error(ZorkaLogger.ZAG_ERRORS, "Error evaluating '" + expr + "': ", e);
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
        ZorkaBshWorker worker = new ZorkaBshWorker(mainExecutor, timeout, this, expr, callback);
        connExecutor.execute(worker);
    }


    /**
     * Loads and executes beanshell script.
     *
     * @param path path to script
     */
    public String loadScript(String path) {
        try {
            log.info(ZorkaLogger.ZAG_CONFIG, "Executing script: " + path);
            interpreter.source(path);
            loadedScripts.add(path);
            return "OK";
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error loading script " + path, e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return "Error: " + e.getMessage();
        } catch (EvalError e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error executing script " + path, e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return "Error: " + e.getMessage();
        }
    }


    public String require(String path) {
        if (!loadedScripts.contains(path)) {
            return loadScript(path);
        } else {
            return "Already loaded.";
        }
    }


    /**
     * Loads and executes all script in script directory.
     */
    public void loadScripts() {
        String scriptsDir = config.stringCfg(AgentConfig.PROP_SCRIPTS_DIR, null);

        if (scriptsDir == null) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Scripts directory not set. Internal error ?!?");
            return;
        }

        List<String> scripts = config.listCfg("scripts");

        if (scripts != null) {
            for (String script : scripts) {
                require(ZorkaUtil.path(scriptsDir, script));
            }
        }
    }


    public void initialize() {
        loadScripts();
        initialized = true;
    }


    public boolean isInitialized() {
        return initialized;
    }


    /**
     * Returns zorka standard library.
     *
     * @return zorka library instance.
     */
    public ZorkaLib getZorkaLib() {
        return zorkaLib;
    }

}
