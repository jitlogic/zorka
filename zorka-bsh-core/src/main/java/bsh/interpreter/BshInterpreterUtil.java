package bsh.interpreter;

import bsh.*;
import bsh.ast.BSHAssignment;
import bsh.ast.SimpleNode;

/**
 * This file is a part of ZOOLA - an extensible BeanShell implementation.
 * Zoola is based on original BeanShell code created by Pat Niemeyer.
 * <p/>
 * Original BeanShell code is Copyright (C) 2000 Pat Niemeyer <pat@pat.net>.
 * <p/>
 * New portions are Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZOOLA. If not, see <http://www.gnu.org/licenses/>.
 */
public class BshInterpreterUtil {

    public static boolean evaluateCondition(
		SimpleNode condExp, BshEvaluatingVisitor visitor)
		throws EvalError
    {
        CallStack callstack = visitor.getCallstack();
        Interpreter interpreter = visitor.getInterpreter();
        Object obj = condExp.accept(visitor);
        if(obj instanceof Primitive) {
			if ( obj == Primitive.VOID )
				throw new EvalError("Condition evaluates to void type",
					condExp, callstack );
            obj = ((Primitive)obj).getValue();
		}

        if(obj instanceof Boolean)
            return ((Boolean)obj).booleanValue();
        else
            throw new EvalError(
				"Condition must evaluate to a Boolean or boolean.",
				condExp, callstack );
    }

    /**
     */
    public static int getIndexAux(
            Object obj, BshEvaluatingVisitor visitor,
            SimpleNode callerInfo )
            throws EvalError
    {
        if ( !obj.getClass().isArray() )
            throw new EvalError("Not an array", callerInfo, visitor.getCallstack() );

        int index;
        try {
            Object indexVal =
                    ((SimpleNode)callerInfo.jjtGetChild(0)).accept(visitor);
            if ( !(indexVal instanceof Primitive) )
                indexVal = Types.castObject(
                        indexVal, Integer.TYPE, Types.ASSIGNMENT);
            index = ((Primitive)indexVal).intValue();
        } catch( UtilEvalError e ) {
            Interpreter.debug("doIndex: "+e);
            throw e.toEvalError(
                    "Arrays may only be indexed by integer types.",
                    callerInfo, visitor.getCallstack() );
        }

        return index;
    }

    public static Object operation(BSHAssignment node, Object lhs, Object rhs, int kind )
            throws UtilEvalError
    {
        /*
              Implement String += value;
              According to the JLS, value may be anything.
              In BeanShell, we'll disallow VOID (undefined) values.
              (or should we map them to the empty string?)
          */
        if ( lhs instanceof String && rhs != Primitive.VOID ) {
            if ( kind != ParserConstants.PLUS )
                throw new UtilEvalError(
                        "Use of non + operator with String LHS" );

            return (String)lhs + rhs;
        }

        if ( lhs instanceof Primitive || rhs instanceof Primitive )
            if(lhs == Primitive.VOID || rhs == Primitive.VOID)
                throw new UtilEvalError(
                        "Illegal use of undefined object or 'void' literal" );
            else if ( lhs == Primitive.NULL || rhs == Primitive.NULL )
                throw new UtilEvalError(
                        "Illegal use of null object or 'null' literal" );


        if( (lhs instanceof Boolean || lhs instanceof Character ||
                lhs instanceof Number || lhs instanceof Primitive) &&
                (rhs instanceof Boolean || rhs instanceof Character ||
                        rhs instanceof Number || rhs instanceof Primitive) )
        {
            return Primitive.binaryOperation(lhs, rhs, kind);
        }

        throw new UtilEvalError("Non primitive value in operator: " +
                lhs.getClass() + " " + node.tokenImage[kind] + " " + rhs.getClass() );
    }

    public static String getTypeDescriptor( Class clas )
    {
        if ( clas == Boolean.TYPE ) return "Z";
        if ( clas == Character.TYPE ) return "C";
        if ( clas == Byte.TYPE ) return "B";
        if ( clas == Short.TYPE ) return "S";
        if ( clas == Integer.TYPE ) return "I";
        if ( clas == Long.TYPE ) return "J";
        if ( clas == Float.TYPE ) return "F";
        if ( clas == Double.TYPE ) return "D";
        if ( clas == Void.TYPE ) return "V";
        // Is getName() ok?  test with 1.1
        String name = clas.getName().replace('.','/');

        if ( name.startsWith("[") || name.endsWith(";") )
            return name;
        else
            return "L"+ name.replace('.','/') +";";
    }

}
