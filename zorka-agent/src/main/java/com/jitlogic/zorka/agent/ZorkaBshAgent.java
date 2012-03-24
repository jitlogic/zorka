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
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import bsh.EvalError;
import bsh.Interpreter;

public class ZorkaBshAgent implements ZorkaService {
	
	public static final String VERSION = "0.0.1-SNAPSHOT";
	
	private static ZorkaLogger log = ZorkaLogger.getLogger(ZorkaBshAgent.class);
	private Interpreter interpreter;
	private ZorkaLib zorkaLib;
	private Executor executor;
	private URL configDir = null;
	
	public ZorkaBshAgent(Executor executor) {
		this.interpreter = new Interpreter();
		this.executor = executor;
		
		zorkaLib = new ZorkaLib(this);
		zorkaLib.addServer("java", ManagementFactory.getPlatformMBeanServer());
		svcAdd(zorkaLib);

		try {
			interpreter.set("zorka", zorkaLib);
		} catch (EvalError e) {
			log.error("Error adding zorka lib to global namespace", e);
		}
	}
	
	
	public String query(String expr) {
		try {
			return ""+interpreter.eval(expr);
		} catch (EvalError e) {
			log.error("Error evaluating '" + expr + "': ", e);
			return ZorkaUtil.errorDump(e);
		}
	}
	
	
	public Object eval(String expr) throws EvalError {
		return interpreter.eval(expr);
	}
	
	
	public void exec(String expr, ZorkaCallback callback) {
		log.debug("Executing ZORKA query: '" + expr + "'"); // TODO avoid concatenation when log level > 0 (? on ZorkaLogger level ?)
		ZorkaBshWorker worker = new ZorkaBshWorker(this, expr, callback);
		executor.execute(worker);
	}
	
	
	public void loadScript(URL url) {
		try {
			interpreter.source(url.getPath());
		} catch (Exception e) {
			log.error("Error loading script " + url, e);
		}
	}
	
	public void loadScriptDir(URL url) {
		try {
			configDir = url;
			File dir = new File(url.toURI());
			String[] files = dir.list();
			if (files == null || files.length == 0) {
				return;
			}
			Arrays.sort(files);
			for (String fname : files) {
				URL scrUrl = new URL(url + "/" + fname);
				File scrFile = new File(scrUrl.toURI());
				if (fname.endsWith(".bsh") && scrFile.isFile()) {
					loadScript(scrUrl);
				}
			}
		} catch (Exception e) {
			log.error("Cannot open directory: " + url, e);
		}
	}
	
	
	public ZorkaLib getZorkaLib() {
		return zorkaLib;
	}
	
	
	public Executor getExecutor() {
		return executor;
	}
	
	
	private boolean svcStarted = false;
	private Set<ZorkaService> services = new HashSet<ZorkaService>(10);
	
	
	public synchronized void svcStart() {
		if (!svcStarted) {
			for (ZorkaService svc : services) {
				svc.svcStart();
			}
			svcStarted = true;
		}
	}
	
	
	public synchronized void svcStop() {
		if (svcStarted) {
			for (ZorkaService svc : services) {
				svc.svcStop();
			}
			svcStarted = false;
		}
	}
	
	
	public synchronized void svcClear() {
		if (svcStarted) {
			for (ZorkaService svc : services) {
				svc.svcClear();
			}
		}
	}
	
	
	public synchronized void svcReload() {
		if (svcStarted) {
			for (ZorkaService svc : services) {
				svc.svcStop();
			}
			
			interpreter = new Interpreter();
			
			if (configDir != null) {
				loadScriptDir(configDir);
			}
			
			for (ZorkaService svc : services) {
				svc.svcStart();
			}
		}
	}
	
	
	public synchronized void svcAdd(ZorkaService service) {
		services.add(service);
		if (svcStarted) { 
			service.svcStart();
		}
	}
}
