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

public class BSHUnaryExpression extends SimpleNode implements ParserConstants
{
    public int kind;
	public boolean postfix = false;

    public BSHUnaryExpression(int id) { super(id); }

    public Object lhsUnaryOperation( LHS lhs, boolean strictJava )
		throws UtilEvalError
    {
        if ( Interpreter.DEBUG ) Interpreter.debug("lhsUnaryOperation");
        Object prevalue, postvalue;
        prevalue = lhs.getValue();
        postvalue = unaryOperation(prevalue, kind);

		Object retVal;
		if ( postfix )
			retVal = prevalue;
		else
			retVal = postvalue;

		lhs.assign( postvalue, strictJava );
		return retVal;
    }

    public Object unaryOperation( Object op, int kind ) throws UtilEvalError
    {
        if (op instanceof Boolean || op instanceof Character 
			|| op instanceof Number)
            return primitiveWrapperUnaryOperation( op, kind );

        if ( !(op instanceof Primitive) )
            throw new UtilEvalError( "Unary operation " + tokenImage[kind]
                + " inappropriate for object" );

		
        return Primitive.unaryOperation((Primitive)op, kind);
    }

    private Object primitiveWrapperUnaryOperation(Object val, int kind)
        throws UtilEvalError
    {
        Class operandType = val.getClass();
        Object operand = Primitive.promoteToInteger(val);

        if ( operand instanceof Boolean )
			return new Boolean(
				Primitive.booleanUnaryOperation((Boolean)operand, kind));
        else 
		if ( operand instanceof Integer )
        {
            int result = Primitive.intUnaryOperation((Integer)operand, kind);

            // ++ and -- must be cast back the original type
            if(kind == INCR || kind == DECR)
            {
                if(operandType == Byte.TYPE)
                    return new Byte((byte)result);
                if(operandType == Short.TYPE)
                    return new Short((short)result);
                if(operandType == Character.TYPE)
                    return new Character((char)result);
            }

            return new Integer(result);
        }
        else if(operand instanceof Long)
            return new Long(Primitive.longUnaryOperation((Long)operand, kind));
        else if(operand instanceof Float)
            return new Float(Primitive.floatUnaryOperation((Float)operand, kind));
        else if(operand instanceof Double)
            return new Double(Primitive.doubleUnaryOperation((Double)operand, kind));
        else
            throw new InterpreterError("An error occurred.  Please call technical support.");
    }

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
