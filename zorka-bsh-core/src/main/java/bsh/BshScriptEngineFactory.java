/**
 *
 * This file is a part of ZOOLA - an extensible BeanShell implementation.
 * Zoola is based on original BeanShell code created by Pat Niemeyer.
 *
 * Original BeanShell code is Copyright (C) 2000 Pat Niemeyer <pat@pat.net>.
 *
 * New portions are Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZOOLA. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package bsh;

import javax.script.ScriptEngine;
import java.util.List;
import java.util.Arrays;

// 	Adopted from http://ikayzo.org/svn/beanshell/BeanShell/engine/src/bsh/engine/BshScriptEngineFactory.java
public class BshScriptEngineFactory implements javax.script.ScriptEngineFactory {
	// Begin impl ScriptEnginInfo

	final List<String> extensions = Arrays.asList("bsh", "java");

	final List<String> mimeTypes = Arrays.asList("application/x-beanshell", "application/x-bsh", "application/x-java-source");

	final List<String> names = Arrays.asList("beanshell", "bsh", "java");


	public String getEngineName() {
		return "BeanShell Engine";
	}


	public String getEngineVersion() {
		return Interpreter.VERSION;
	}


	public List<String> getExtensions() {
		return extensions;
	}


	public List<String> getMimeTypes() {
		return mimeTypes;
	}


	public List<String> getNames() {
		return names;
	}


	public String getLanguageName() {
		return "BeanShell";
	}


	public String getLanguageVersion() {
		return bsh.Interpreter.VERSION + "";
	}


	public Object getParameter(String param) {
		if (param.equals(ScriptEngine.ENGINE)) {
			return getEngineName();
		}
		if (param.equals(ScriptEngine.ENGINE_VERSION)) {
			return getEngineVersion();
		}
		if (param.equals(ScriptEngine.NAME)) {
			return getEngineName();
		}
		if (param.equals(ScriptEngine.LANGUAGE)) {
			return getLanguageName();
		}
		if (param.equals(ScriptEngine.LANGUAGE_VERSION)) {
			return getLanguageVersion();
		}
		if (param.equals("THREADING")) {
			return "MULTITHREADED";
		}

		return null;
	}


	public String getMethodCallSyntax(String objectName, String methodName, String... args) {
		// Note: this is very close to the bsh.StringUtil.methodString()
		// method, which constructs a method signature from arg *types*.  Maybe
		// combine these later.

		StringBuffer sb = new StringBuffer();
		if (objectName != null) {
			sb.append(objectName).append('.');
		}
		sb.append(methodName).append('(');
		if (args.length > 0) {
			sb.append(' ');
		}
		for (int i = 0; i < args.length; i++) {
			sb.append((args[i] == null) ? "null" : args[i]).append(i < (args.length - 1) ? ", " : " ");
		}
		sb.append(")");
		return sb.toString();
	}


	public String getOutputStatement(String message) {
		return "print( \"" + message + "\" );";
	}


	public String getProgram(String... statements) {
		StringBuffer sb = new StringBuffer();
		for (final String statement : statements) {
			sb.append(statement);
			if ( ! statement.endsWith(";")) {
				sb.append(";");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	// End impl ScriptEngineInfo

	// Begin impl ScriptEngineFactory


	public ScriptEngine getScriptEngine() {
		return new BshScriptEngine();
	}

	// End impl ScriptEngineFactory
}

