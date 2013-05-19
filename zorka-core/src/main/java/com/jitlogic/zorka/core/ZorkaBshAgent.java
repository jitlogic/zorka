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
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.util.ObjectDumper;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import com.jitlogic.zorka.core.util.ZorkaUtil;
import com.jitlogic.zorka.core.util.ZorkaLog;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * This is central part of Zorka agent - it processes actual queries and executes BSH scripts.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaBshAgent {

    /** Logger */
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** Beanshell interpreter */
	private Interpreter interpreter;

    /** Zorka standard library */
	private ZorkaLib zorkaLib;

    /** Executor for asynchronous processing queries */
	private Executor connExecutor;

    private ExecutorService mainExecutor;

    private long timeout;

    private boolean initialized;

    /**
     * Standard constructor.
     *
     * @param connExecutor connExecutor for asynchronous processing queries
     */
	public ZorkaBshAgent(Executor connExecutor, ExecutorService mainExecutor, long timeout, MBeanServerRegistry mbsRegistry,
                         ZorkaConfig config, QueryTranslator translator) {
		this.interpreter = new Interpreter();

		this.connExecutor = connExecutor;
        this.mainExecutor = mainExecutor;
        this.timeout = timeout;


		zorkaLib = new ZorkaLib(this, mbsRegistry, config, translator);
        install("zorka", zorkaLib);
    }


    /**
     * Installs object in beanshell namespace. Typically used to install
     * objects as function libraries.
     *
     * @param name name in beanshell namespace
     *
     * @param obj object
     */
    public void install(String name, Object obj) {
        try {
            interpreter.set(name, obj);
        } catch (EvalError e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error adding module '" + name + "' to global namespace", e);
        }
    }


    /**
     * Evaluates BSH query. If error occurs, it returns exception text with stack dump.
     *
     * @param expr query string
     *
     * @return response string
     */
    public String query(String expr) {
		try {
			return ""+interpreter.eval(expr); // TODO proper object-to-string conversion
		} catch (EvalError e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error evaluating '" + expr + "': ", e);
			return ObjectDumper.errorDump(e);
		}
	}


    /**
     * Evaluates BSH query. If evaluation error occurs, it is thrown out as EvalError.
     *
     * @param expr query string
     *
     * @return evaluation result
     *
     * @throws EvalError
     */
	public Object eval(String expr) throws EvalError {
		return interpreter.eval(expr);
	}


    /**
     * Executes query asynchronously. Result is returned via callback object.
     *
     * @param expr BSH expression
     *
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



    /**
     * Loads and executes all script in given directory matching given mask.
     *
     * @param path path to directory
     *
     * @param mask file mas (eg. *.bsh)
     */
    public String loadScriptDir(String path, String mask) {
        StringBuilder sb = new StringBuilder();
        try {
            File dir = new File(path);
            log.info(ZorkaLogger.ZAG_CONFIG, "Listing directory: " + path);
            String[] files = dir.list();
            if (files == null || files.length == 0) {
                return "No files in " + path + " directory.\n";
            }
            Arrays.sort(files);
            for (String fname : files) {
                if (!fname.matches(mask)) {
                    continue;
                }
                String scrPath = ZorkaUtil.path(path, fname);
                File scrFile = new File(scrPath);
                if (fname.endsWith(".bsh") && scrFile.isFile()) {
                    sb.append("Executed " + fname + ": " + loadScript(scrPath) + "\n");
                } else {
                    log.info(ZorkaLogger.ZAG_CONFIG, "Skipping file '" + scrPath + ": isFile=" + scrFile.isFile());
                    sb.append("Skipped " + fname + "." + "\n");
                }
            }
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot open directory: " + path, e);
        }

        return sb.toString();
    }


    public void initialize(String configDir) {
        loadScriptDir(configDir, ".*\\.bsh$");
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
