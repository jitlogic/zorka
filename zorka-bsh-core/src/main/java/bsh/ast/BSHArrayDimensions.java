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

/**
	The name of this class is somewhat misleading.  This covers both the case
	where there is an array initializer and 
*/
public class BSHArrayDimensions extends SimpleNode
{
	public Class baseType;
    public int numDefinedDims;
    public int numUndefinedDims;
	/** 
		The Length in each defined dimension.  This value set by the eval() 
		Since the values can come from Expressions we should be re-eval()d each
		time.
	*/
	public int [] definedDimensions;  

    public BSHArrayDimensions(int id) { super(id); }

    public void addDefinedDimension() { numDefinedDims++; }
    public void addUndefinedDimension() { numUndefinedDims++; }

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
