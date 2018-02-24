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

public class BSHFormalParameters extends SimpleNode
{
	private String [] paramNames;
	/**
		For loose type parameters the paramTypes are null.
	*/
	// unsafe caching of types
	public Class [] paramTypes;
	public int numArgs;
	public String [] typeDescriptors;

	public BSHFormalParameters(int id) { super(id); }

	public void insureParsed()
	{
		if ( paramNames != null )
			return;

		this.numArgs = jjtGetNumChildren();
		String [] paramNames = new String[numArgs];

		for(int i=0; i<numArgs; i++)
		{
			BSHFormalParameter param = (BSHFormalParameter)jjtGetChild(i);
			paramNames[i] = param.name;
		}

		this.paramNames = paramNames;
	}

	public String [] getParamNames() { 
		insureParsed();
		return paramNames;
	}

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}

