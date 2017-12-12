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

package bsh.ast;

import bsh.*;
import bsh.interpreter.BshEvaluatingVisitor;

public class BSHMethodDeclaration extends SimpleNode
{
	public String name;

	// Begin Child node structure evaluated by insureNodesParsed

	public BSHReturnType returnTypeNode;
	public BSHFormalParameters paramsNode;
	public BSHBlock blockNode;
	// index of the first throws clause child node
	public int firstThrowsClause;

	// End Child node structure evaluated by insureNodesParsed

	public Modifiers modifiers;

	// Unsafe caching of type here.
	public Class returnType;  // null (none), Void.TYPE, or a Class
	public int numThrows = 0;

	public BSHMethodDeclaration(int id) { super(id); }

	/**
		Set the returnTypeNode, paramsNode, and blockNode based on child
		node structure.  No evaluation is done here.
	*/
	public synchronized void insureNodesParsed()
	{
		if ( paramsNode != null ) // there is always a paramsNode
			return;

		Object firstNode = jjtGetChild(0);
		firstThrowsClause = 1;
		if ( firstNode instanceof BSHReturnType )
		{
			returnTypeNode = (BSHReturnType)firstNode;
			paramsNode = (BSHFormalParameters)jjtGetChild(1);
			if ( jjtGetNumChildren() > 2+numThrows )
				blockNode = (BSHBlock)jjtGetChild(2+numThrows); // skip throws
			++firstThrowsClause;
		}
		else
		{
			paramsNode = (BSHFormalParameters)jjtGetChild(0);
			blockNode = (BSHBlock)jjtGetChild(1+numThrows); // skip throws
		}
	}


	public String toString() {
		return "MethodDeclaration: "+name;
	}

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
