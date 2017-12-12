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

import bsh.classpath.ClassManagerImpl;
import bsh.interpreter.BshEvaluatingVisitor;

import java.io.*;
import java.util.Map;

/**
 * With this class the script source is only parsed once and the resulting AST is used for
 * {@link #invoke(java.util.Map) every invocation}. This class is designed to be thread-safe.  
 */
public class PreparsedScript {

	private final BshMethod _method;
	private final Interpreter _interpreter;


	public PreparsedScript(final String source) throws EvalError {
		this(source, getDefaultClassLoader());
	}


	private static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (final SecurityException e) {
			// ignore
		}
		if (cl == null) {
			cl = PreparsedScript.class.getClassLoader();
		}
		if (cl != null) {
			return cl;
		}
		return ClassLoader.getSystemClassLoader();
	}


	public PreparsedScript(final String source, final ClassLoader classLoader) throws EvalError {
		final ClassManagerImpl classManager = new ClassManagerImpl();
		classManager.setClassLoader(classLoader);
		final NameSpace nameSpace = new NameSpace(classManager, "global");
		_interpreter = new Interpreter(new StringReader(""), System.out, System.err, false, nameSpace, null, null);
		try {
			final This callable = (This) _interpreter.eval("__execute() { " + source + "\n" + "}\n" + "return this;");
			_method = callable.getNameSpace().getMethod("__execute", new Class[0], false);
		} catch (final UtilEvalError e) {
			throw new IllegalStateException(e);
		}
	}


	public Object invoke(final Map<String, ?> context) throws EvalError {
		final NameSpace nameSpace = new NameSpace(_interpreter.getClassManager(), "BeanshellExecutable");
		nameSpace.setParent(_interpreter.getNameSpace());
		final BshMethod method = new BshMethod(_method.getName(), _method.getReturnType(), _method.getParameterNames(), _method.getParameterTypes(), _method.methodBody, nameSpace, _method.getModifiers());
		for (final Map.Entry<String, ?> entry : context.entrySet()) {
			try {
				nameSpace.setVariable(entry.getKey(), entry.getValue(), false);
			} catch (final UtilEvalError e) {
				throw new EvalError("cannot set variable '" + entry.getKey() + '\'', null, null, e);
			}
		}
		final Object result = method.invoke(new Object[0], new BshEvaluatingVisitor(null, _interpreter));
		if (result instanceof Primitive) {
			if (( (Primitive) result).getType() == Void.TYPE) {
				return null;
			}
			return ( (Primitive) result).getValue();
		}
		return result;
	}


	public void setOut(final PrintStream value) {
		_interpreter.setOut(value);
	}


	public void setErr(final PrintStream value) {
		_interpreter.setErr(value);
	}

}
