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

import bsh.ast.BSHBlock;
import bsh.ast.BSHFormalParameters;
import bsh.ast.BSHReturnType;
import bsh.interpreter.BshEvaluatingVisitor;

public class DelayedEvalBshMethod extends BshMethod
{
	String returnTypeDescriptor;
	BSHReturnType returnTypeNode;
	String [] paramTypeDescriptors;
	BSHFormalParameters paramTypesNode;

	// used for the delayed evaluation...
	//transient CallStack callstack;
	//transient Interpreter interpreter;
    transient BshEvaluatingVisitor evaluator;

    /**
		This constructor is used in class generation.  It supplies String type
		descriptors for return and parameter class types and allows delay of 
		the evaluation of those types until they are requested.  It does this
		by holding BSHType nodes, as well as an evaluation callstack, and
		interpreter which are called when the class types are requested. 
	*/
	/*
		Note: technically I think we could get by passing in only the
		current namespace or perhaps BshClassManager here instead of 
		CallStack and Interpreter.  However let's just play it safe in case
		of future changes - anywhere you eval a node you need these.
	*/
	DelayedEvalBshMethod( 
		String name, 
		String returnTypeDescriptor, BSHReturnType returnTypeNode,
		String [] paramNames,
		String [] paramTypeDescriptors, BSHFormalParameters paramTypesNode,
		BSHBlock methodBody,
		NameSpace declaringNameSpace, Modifiers modifiers,
		BshEvaluatingVisitor visitor
	) {
		super( name, null/*returnType*/, paramNames, null/*paramTypes*/,
			methodBody, declaringNameSpace, modifiers );

		this.returnTypeDescriptor = returnTypeDescriptor;
		this.returnTypeNode = returnTypeNode;
		this.paramTypeDescriptors = paramTypeDescriptors;
		this.paramTypesNode = paramTypesNode;

        this.evaluator = visitor;

        //this.evaluator = new BshEvaluatingVisitor(callstack, interpreter);
	}

	public String getReturnTypeDescriptor() { return returnTypeDescriptor; }

	public Class getReturnType() 
	{ 
		if ( returnTypeNode == null )
			return null;

		// BSHType will cache the type for us
		try {
			return evaluator.evalReturnType( returnTypeNode);
		} catch ( EvalError e ) {
			throw new InterpreterError("can't eval return type: "+e);
		}
	}

	public String [] getParamTypeDescriptors() { return paramTypeDescriptors; }

	public Class [] getParameterTypes() 
	{ 
		// BSHFormalParameters will cache the type for us
		try {
            return (Class[])paramTypesNode.accept(evaluator);
		} catch ( EvalError e ) {
			throw new InterpreterError("can't eval param types: "+e);
		}
	}
}
