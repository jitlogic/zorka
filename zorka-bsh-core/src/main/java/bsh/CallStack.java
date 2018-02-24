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

import java.io.Serializable;
import java.util.Stack;
import java.util.EmptyStackException;

/**
	A stack of NameSpaces representing the call path.
	Each method invocation, for example, pushes a new NameSpace onto the stack.
	The top of the stack is always the current namespace of evaluation.
	<p>

	This is used to support the this.caller magic reference and to print
	script "stack traces" when evaluation errors occur.
	<p>

	Note: How can this be thread safe, you might ask?  Wouldn't a thread 
	executing various beanshell methods be mutating the callstack?  Don't we 
	need one CallStack per Thread in the interpreter?  The answer is that we do.
	Any java.lang.Thread enters our script via an external (hard) Java 
	reference via a This type interface, e.g.  the Runnable interface 
	implemented by This or an arbitrary interface implemented by XThis.  
	In that case the This invokeMethod() method (called by any interface that 
	it exposes) creates a new CallStack for each external call.
	<p>
*/
public final class CallStack implements Serializable {

	private static final long serialVersionUID = 0L;

	private final Stack<NameSpace> stack = new Stack<NameSpace>();


	public CallStack() { }

	public CallStack( NameSpace namespace ) { 
		push( namespace );
	}

	public void clear() {
		stack.removeAllElements();
	}

	public void push( NameSpace ns ) {
		stack.push( ns );
	}

	public NameSpace top() {
		return stack.peek();
	}

	/**
		zero based.
	*/
	public NameSpace get(int depth) {
		int size = stack.size();
		if ( depth >= size )
			return NameSpace.JAVACODE;
		else
			return stack.get(size-1-depth);
	}
	
	/**
		This is kind of crazy, but used by the setNameSpace command.
		zero based.
	*/
	public void set(int depth, NameSpace ns) {
		stack.set( stack.size()-1-depth, ns );
	}

	public NameSpace pop() {
		try {
			return stack.pop();
		} catch(EmptyStackException e) {
			throw new InterpreterError("pop on empty CallStack");
		}
	}

	/**
		Swap in the value as the new top of the stack and return the old
		value.
	*/
	public NameSpace swap( NameSpace newTop ) {
		int last = stack.size() - 1;
		NameSpace oldTop = stack.get(last);
		stack.set( last, newTop );
		return oldTop;
	}

	public int depth() {
		return stack.size();
	}
/*
	public NameSpace [] toArray() {
		NameSpace [] nsa = new NameSpace [ depth() ];
		stack.copyInto( nsa );
		return nsa;
	}
*/
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CallStack:\n");
		for( int i=stack.size()-1; i>=0; i-- )
			sb.append("\t"+stack.get(i)+"\n");

		return sb.toString();
	}

	/**
		Occasionally we need to freeze the callstack for error reporting
		purposes, etc.
	*/
	public CallStack copy() {
		CallStack cs = new CallStack();
		cs.stack.addAll(this.stack);
		return cs;
	}
}
