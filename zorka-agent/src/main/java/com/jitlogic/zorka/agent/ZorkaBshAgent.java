/** 
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executor;

import com.jitlogic.zorka.util.ObjectDumper;
import com.jitlogic.zorka.util.ZorkaLog;

import bsh.EvalError;
import bsh.Interpreter;
import com.jitlogic.zorka.integ.ZorkaLogger;

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
	private Executor executor;

    /**
     * Standard constructor.
     *
     * @param executor executor for asynchronous processing queries
     */
	public ZorkaBshAgent(Executor executor) {
		this.interpreter = new Interpreter();
		this.executor = executor;

		zorkaLib = new ZorkaLib(this);
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
            log.error("Error adding module '" + name + "' to global namespace", e);
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
			log.error("Error evaluating '" + expr + "': ", e);
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
		ZorkaBshWorker worker = new ZorkaBshWorker(this, expr, callback);
		executor.execute(worker);
	}


    /**
     * Loads and executes beanshell script.
     *
     * @param url path to script
     */
	public void loadScript(URL url) {
		try {
			interpreter.source(url.getPath());
		} catch (Exception e) {
			log.error("Error loading script " + url, e);
		} catch (EvalError e) {
            log.error("Error executing script " + url, e);
        }
	}


    /**
     * Loads and executes beanshell script.
     *
     * @param path path to script
     */
    public void loadScript(String path) {
        try {
            interpreter.source(path);
        } catch (Exception e) {
            log.error("Error loading script " + path, e);
        }
    }


    /**
     * Loads and executes all .bsh files from given directory.
     * Scripts are executed in alphabetical order.
     *
     * @param url url to script directory
     */
    public void loadScriptDir(URL url) {
		try {
			File dir = new File(url.getPath());
            log.debug("Listing directory: " + url.getPath());
			String[] files = dir.list();
			if (files == null || files.length == 0) {
				return;
			}
			Arrays.sort(files);
			for (String fname : files) {
				URL scrUrl = new URL(url + "/" + fname);
                log.debug("Loading file: " + scrUrl.getPath());
				File scrFile = new File(scrUrl.getPath());
				if (fname.endsWith(".bsh") && scrFile.isFile()) {
					loadScript(scrUrl);
				}
			}
		} catch (Exception e) {
			log.error("Cannot open directory: " + url, e);
		}
	}


    /**
     * Loads and executes all script in given directory matching given mask.
     *
     * @param path path to directory
     *
     * @param mask file mas (eg. *.bsh)
     */
    public void loadScriptDir(String path, String mask) {
        try {
            File dir = new File(path);
            log.debug("Listing directory: " + path);
            String[] files = dir.list();
            if (files == null || files.length == 0) {
                return;
            }
            Arrays.sort(files);
            for (String fname : files) {
                if (!fname.matches(mask)) {
                    continue;
                }
                String scrPath = path + "/" + fname;
                log.debug("Loading file: " + scrPath);
                File scrFile = new File(scrPath);
                if (fname.endsWith(".bsh") && scrFile.isFile()) {
                    loadScript(scrPath);
                }
            }
        } catch (Exception e) {
            log.error("Cannot open directory: " + path, e);
        }
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
