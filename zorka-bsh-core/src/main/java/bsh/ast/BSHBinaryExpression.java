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

/**
	Implement binary expressions...
	Note: this is too complicated... need some cleanup and simplification.
	@see bsh.Primitive
*/
public class BSHBinaryExpression extends SimpleNode
	implements ParserConstants
{
    public int kind;

    public BSHBinaryExpression(int id) { super(id); }

	/*
		object is a non-null and non-void Primitive type
	*/
	public boolean isPrimitiveValue( Object obj ) {
        return ( (obj instanceof Primitive) 
			&& (obj != Primitive.VOID) && (obj != Primitive.NULL) );
	}

	/*
		object is a java.lang wrapper for boolean, char, or number type
	*/
	public boolean isWrapper( Object obj ) {
        return ( obj instanceof Boolean || 
			obj instanceof Character || obj instanceof Number );
	}

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
