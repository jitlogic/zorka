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

import bsh.ast.SimpleNode;

/**
	EvalError indicates that we cannot continue evaluating the script
	or the script has thrown an exception.

	EvalError may be thrown for a script syntax error, an evaluation 
	error such as referring to an undefined variable, an internal error.
	<p>
	
	@see TargetError
*/
public class EvalError extends Error
{
	private SimpleNode node;

	// Note: no way to mutate the Throwable message, must maintain our own
	private String message;

	private final CallStack callstack;

	public EvalError( String s, SimpleNode node, CallStack callstack, Throwable cause ) {
		this(s,node,callstack);
		initCause(cause);
	}

	public EvalError( String s, SimpleNode node, CallStack callstack ) {
		this.message = s;
		this.node = node;
		// freeze the callstack for the stack trace.
		this.callstack = callstack==null ? null : callstack.copy();
	}

	/**
		Print the error with line number and stack trace.
	*/
	public String getMessage() 
	{
		String trace;
		if ( node != null )
			trace = " : at Line: "+ node.getLineNumber() 
				+ " : in file: "+ node.getSourceFile()
				+ " : "+node.getText();
		else
			// Users should not normally see this.
			trace = ": <at unknown location>";

		if ( callstack != null )
			trace = trace +"\n" + getScriptStackTrace();

		return getRawMessage() + trace;
	}

	/**
		Re-throw the error, prepending the specified message.
	*/
	public void reThrow( String msg ) 
		throws EvalError 
	{
		prependMessage( msg );
		throw this;
	}

	/**
		The error has trace info associated with it. 
		i.e. It has an AST node that can print its location and source text.
	*/
	SimpleNode getNode() {
		return node;
	}

	void setNode( SimpleNode node ) {
		this.node = node;
	}

	public String getErrorText() { 
		if ( node != null )
			return node.getText() ;
		else
			return "<unknown error>";
	}

	public int getErrorLineNumber() { 
		if ( node != null )
			return node.getLineNumber() ;
		else
			return -1;
	}

	public String getErrorSourceFile() {
		if ( node != null )
			return node.getSourceFile() ;
		else
			return "<unknown file>";
	}

	public String getScriptStackTrace() 
	{
		if ( callstack == null )
			return "<Unknown>";

		String trace = "";
		CallStack stack = callstack.copy();
		while ( stack.depth() > 0 ) 
		{
			NameSpace ns = stack.pop();
			SimpleNode node = ns.getNode();
			if ( ns.isMethod )
			{
				trace = trace + "\nCalled from method: " + ns.getName();
				if ( node != null )
					trace += " : at Line: "+ node.getLineNumber() 
						+ " : in file: "+ node.getSourceFile()
						+ " : "+node.getText();
			}
		}

		return trace;
	}

	public String getRawMessage() { return message; }

	/**
		Prepend the message if it is non-null.
	*/
	private void prependMessage( String s ) 
	{ 
		if ( s == null )
			return;

		if ( message == null )
			message = s;
		else
			message = s + " : "+ message;
	}

}

