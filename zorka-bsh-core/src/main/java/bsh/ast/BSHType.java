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

import java.lang.reflect.Array;

public class BSHType extends SimpleNode
	implements BshClassManager.Listener
{
	/**
		baseType is used during evaluation of full type and retained for the
		case where we are an array type.
		In the case where we are not an array this will be the same as type.
	*/
	public Class baseType;
	/** 
		If we are an array type this will be non zero and indicate the 
		dimensionality of the array.  e.g. 2 for String[][];
	*/
    public int arrayDims;

	/** 
		Internal cache of the type.  Cleared on classloader change.
	*/
    public Class type;

	public String descriptor;

    public BSHType(int id) {
		super(id); 
	}

	/**
		Used by the grammar to indicate dimensions of array types 
		during parsing.
	*/
    public void addArrayDimension() { 
		arrayDims++; 
	}

	public SimpleNode getTypeNode() {
        return (SimpleNode)jjtGetChild(0);
	}



	/**
		baseType is used during evaluation of full type and retained for the
		case where we are an array type.
		In the case where we are not an array this will be the same as type.
	*/
	public Class getBaseType() {
		return baseType;
	}
	/** 
		If we are an array type this will be non zero and indicate the 
		dimensionality of the array.  e.g. 2 for String[][];
	*/
	public int getArrayDims() {
		return arrayDims;
	}

	public void classLoaderChanged() {
		type = null;
		baseType = null;
	}


    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
