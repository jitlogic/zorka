package bsh.interpreter;

import bsh.*;
import bsh.ast.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class BshEvaluatingVisitor extends BshNodeVisitor<Object> {

    private CallStack callstack;
    private Interpreter interpreter;


    public BshEvaluatingVisitor(CallStack callstack, Interpreter interpreter) {
        this.callstack = callstack;
        this.interpreter = interpreter;
    }


    public CallStack getCallstack() {
        return callstack;
    }


    public Interpreter getInterpreter() {
        return interpreter;
    }

    @Override
    public Object visit(BSHAllocationExpression node) {
        // type is either a class name or a primitive type
        SimpleNode type = (SimpleNode)node.jjtGetChild(0);

        // args is either constructor arguments or array dimensions
        SimpleNode args = (SimpleNode)node.jjtGetChild(1);

        if ( type instanceof BSHAmbiguousName)
        {
            BSHAmbiguousName name = (BSHAmbiguousName)type;

            if (args instanceof BSHArguments)
                return objectAllocation(node, name, (BSHArguments) args);
            else
                return objectArrayAllocation(node, name, (BSHArrayDimensions) args
                );
        }
        else
            return primitiveArrayAllocation(node, (BSHPrimitiveType) type,
                    (BSHArrayDimensions) args);

    }

    public Object objectAllocation(BSHAllocationExpression node,
            BSHAmbiguousName nameNode, BSHArguments argumentsNode)
            throws EvalError
    {
        NameSpace namespace = callstack.top();

        Object[] args = getArguments(argumentsNode);
        if ( args == null)
            throw new EvalError( "Null args in new.", node, callstack );

        // Look for scripted class object
        Object obj = ambiguousNameToObject(nameNode, false/* force class*/ );

        // Try regular class

        obj = ambiguousNameToObject( nameNode, true/*force class*/ );

        Class type = null;
        if ( obj instanceof ClassIdentifier )
            type = ((ClassIdentifier)obj).getTargetClass();
        else
            throw new EvalError(
                    "Unknown class: "+nameNode.text, node, callstack );

        // Is an inner class style object allocation
        boolean hasBody = node.jjtGetNumChildren() > 2;

        if ( hasBody )
        {
            BSHBlock body = (BSHBlock)node.jjtGetChild(2);
            if ( type.isInterface() )
                return constructWithInterfaceBody(node,
                        type, args, body);
            else
                return constructWithClassBody( node,
                        type, args, body );
        } else
            return constructObject(node, type, args );
    }


    public Object constructObject(BSHAllocationExpression node, Class<?> type, Object[] args ) throws EvalError {
        final boolean isGeneratedClass = GeneratedClass.class.isAssignableFrom(type);
        if (isGeneratedClass) {
            ClassGeneratorUtil.registerConstructorContext(callstack, interpreter);
        }
        Object obj;
        try {
            obj = Reflect.constructObject( type, args );
        } catch ( ReflectError e) {
            throw new EvalError(
                    "Constructor error: " + e.getMessage(), node, callstack );
        } catch (InvocationTargetException e) {
            // No need to wrap this debug
            Interpreter.debug("The constructor threw an exception:\n\t" + e.getTargetException());
            throw new TargetError("Object constructor", e.getTargetException(), node, callstack, true);
        } finally {
            if (isGeneratedClass) {
                ClassGeneratorUtil.registerConstructorContext(null, null); // clean up, prevent memory leak
            }
        }

        String className = type.getName();
        // Is it an inner class?
        if ( className.indexOf("$") == -1 )
            return obj;

        // Temporary hack to support inner classes
        // If the obj is a non-static inner class then import the context...
        // This is not a sufficient emulation of inner classes.
        // Replace this later...

        // work through to class 'this'
        This ths = callstack.top().getThis( null );
        NameSpace instanceNameSpace =
                Name.getClassNameSpace( ths.getNameSpace() );

        // Change the parent (which was the class static) to the class instance
        // We really need to check if we're a static inner class here first...
        // but for some reason Java won't show the static modifier on our
        // fake inner classes...  could generate a flag field.
        if ( instanceNameSpace != null
                && className.startsWith( instanceNameSpace.getName() +"$")
                )
        {
            ClassGenerator.getClassGenerator().setInstanceNameSpaceParent(
                    obj, className, instanceNameSpace );
        }

        return obj;
    }


    public Object constructWithClassBody( BSHAllocationExpression node,
            Class type, Object[] args, BSHBlock block )
            throws EvalError
    {
        String name = callstack.top().getName() + "$" + (++node.innerClassCount);
        Modifiers modifiers = new Modifiers();
        modifiers.addModifier( Modifiers.CLASS, "public" );
        Class clas = ClassGenerator.getClassGenerator() .generateClass(
                name, modifiers, null/*interfaces*/, type/*superClass*/,
                block, false/*isInterface*/, callstack, interpreter );
        try {
            return Reflect.constructObject( clas, args );
        } catch ( Exception e ) {
            Throwable cause = e;
            if ( e instanceof InvocationTargetException ) {
                cause = ((InvocationTargetException) e).getTargetException();
            }
            throw new EvalError("Error constructing inner class instance: "+e, node, callstack, cause);
        }
    }


    public Object constructWithInterfaceBody( BSHAllocationExpression node,
            Class type, Object[] args, BSHBlock body )
            throws EvalError
    {
        NameSpace namespace = callstack.top();
        NameSpace local = new NameSpace(namespace, "AnonymousBlock");
        callstack.push(local);
        evalBlock(body, true);
        callstack.pop();
        // statical import fields from the interface so that code inside
        // can refer to the fields directly (e.g. HEIGHT)
        local.importStatic( type );
        return local.getThis(interpreter).getInterface( type );
    }


    public Object objectArrayAllocation( BSHAllocationExpression node,
            BSHAmbiguousName nameNode, BSHArrayDimensions dimensionsNode )
            throws EvalError
    {
        NameSpace namespace = callstack.top();
        Class type = ambiguousNameToClass( nameNode );
        if ( type == null )
            throw new EvalError( "Class " + nameNode.getName(namespace)
                    + " not found.", node, callstack );

        return arrayAllocation( node, dimensionsNode, type );
    }

    public Object primitiveArrayAllocation( BSHAllocationExpression node,
            BSHPrimitiveType typeNode, BSHArrayDimensions dimensionsNode
    )
            throws EvalError
    {
        Class type = typeNode.getType();

        return arrayAllocation(node, dimensionsNode, type );
    }

    public Object arrayAllocation( BSHAllocationExpression node,
            BSHArrayDimensions dimensionsNode, Class type)
            throws EvalError
    {
        /*
              dimensionsNode can return either a fully intialized array or VOID.
              when VOID the prescribed array dimensions (defined and undefined)
              are contained in the node.
          */
        Object result = evalArrayDimensions(dimensionsNode, type);
        if ( result != Primitive.VOID )
            return result;
        else
            return arrayNewInstance( node, type, dimensionsNode );
    }

    /**
     Create an array of the dimensions specified in dimensionsNode.
     dimensionsNode may contain a number of "undefined" as well as "defined"
     dimensions.
     <p>

     Background: in Java arrays are implemented in arrays-of-arrays style
     where, for example, a two dimensional array is a an array of arrays of
     some base type.  Each dimension-type has a Java class type associated
     with it... so if foo = new int[5][5] then the type of foo is
     int [][] and the type of foo[0] is int[], etc.  Arrays may also be
     specified with undefined trailing dimensions - meaning that the lower
     order arrays are not allocated as objects. e.g.
     if foo = new int [5][]; then foo[0] == null //true; and can later be
     assigned with the appropriate type, e.g. foo[0] = new int[5];
     (See Learning Java, O'Reilly & Associates more background).
     <p>

     To create an array with undefined trailing dimensions using the
     reflection API we must use an array type to represent the lower order
     (undefined) dimensions as the "base" type for the array creation...
     Java will then create the correct type by adding the dimensions of the
     base type to specified allocated dimensions yielding an array of
     dimensionality base + specified with the base dimensons unallocated.
     To create the "base" array type we simply create a prototype, zero
     length in each dimension, array and use it to get its class
     (Actually, I think there is a way we could do it with Class.forName()
     but I don't trust this).   The code is simpler than the explanation...
     see below.
     */
    public Object arrayNewInstance( BSHAllocationExpression node,
            Class type, BSHArrayDimensions dimensionsNode )
            throws EvalError
    {
        if ( dimensionsNode.numUndefinedDims > 0 )
        {
            Object proto = Array.newInstance(
                    type, new int [dimensionsNode.numUndefinedDims] ); // zeros
            type = proto.getClass();
        }

        try {
            return Array.newInstance(
                    type, dimensionsNode.definedDimensions);
        } catch( NegativeArraySizeException e1 ) {
            throw new TargetError( e1, node, callstack );
        } catch( Exception e ) {
            throw new EvalError("Can't construct primitive array: " +
                    e.getMessage(), node, callstack);
        }
    }


    @Override
    public Object visit(BSHAmbiguousName node) {
        throw new InterpreterError(
                "Don't know how to eval an ambiguous name!"
                        +"  Use toObject() if you want an object." );
    }


    public Object ambiguousNameToObject( BSHAmbiguousName node )
            throws EvalError
    {
        return ambiguousNameToObject( node, false );
    }

    public Object ambiguousNameToObject( BSHAmbiguousName node,
            boolean forceClass )
            throws EvalError
    {
        try {
            return
                    node.getName( callstack.top() ).toObject(
                            this, forceClass );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }

    public Class ambiguousNameToClass( BSHAmbiguousName node )
            throws EvalError
    {
        try {
            return node.getName( callstack.top() ).toClass();
        } catch ( ClassNotFoundException e ) {
            throw new EvalError( e.getMessage(), node, callstack, e );
        } catch ( UtilEvalError e2 ) {
            // ClassPathException is a type of UtilEvalError
            throw e2.toEvalError( node, callstack );
        }
    }

    public LHS ambiguousNameToLHS( BSHAmbiguousName node )
            throws EvalError
    {
        try {
            return node.getName( callstack.top() ).toLHS( this );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }


    @Override
    public Object visit(BSHArguments node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHArguments class.");
    }


    /*
         Disallowing VOIDs here was an easy way to support the throwing of a
         more descriptive error message on use of an undefined argument to a
         method call (very common).  If it ever turns out that we need to
         support that for some reason we'll have to re-evaluate how we get
         "meta-information" about the arguments in the various invoke() methods
         that take Object [].  We could either pass BSHArguments down to
         overloaded forms of the methods or throw an exception subtype
         including the argument position back up, where the error message would
         be compounded.
     */
    public Object[] getArguments( BSHArguments node )
            throws EvalError
    {
        // evaluate each child
        Object[] args = new Object[node.jjtGetNumChildren()];
        for(int i = 0; i < args.length; i++)
        {
            args[i] = ((SimpleNode)node.jjtGetChild(i)).accept(this);
            if ( args[i] == Primitive.VOID )
                throw new EvalError( "Undefined argument: " +
                        ((SimpleNode)node.jjtGetChild(i)).getText(), node, callstack );
        }

        return args;
    }


    /**
     Evaluate the structure of the array in one of two ways:

     a) an initializer exists, evaluate it and return
     the fully constructed array object, also record the dimensions
     of that array

     b) evaluate and record the lengths in each dimension and
     return void.

     The structure of the array dims is maintained in dimensions.
     */
    @Override
    public Object visit(BSHArrayDimensions node) {
        SimpleNode child = (SimpleNode)node.jjtGetChild(0);

        /*
              Child is array initializer.  Evaluate it and fill in the
              dimensions it returns.  Initialized arrays are always fully defined
              (no undefined dimensions to worry about).
              The syntax uses the undefinedDimension count.
              e.g. int [][] { 1, 2 };
          */
        if (child instanceof BSHArrayInitializer)
        {
            if ( node.baseType == null )
                throw new EvalError(
                        "Internal Array Eval err:  unknown base type",
                        node, callstack );

            Object initValue = evalArrayInitializer(((BSHArrayInitializer) child),
                    node.baseType, node.numUndefinedDims, callstack, interpreter);

            Class arrayClass = initValue.getClass();
            int actualDimensions = Reflect.getArrayDimensions(arrayClass);
            node.definedDimensions = new int[ actualDimensions ];

            // Compare with number of dimensions actually created with the
            // number specified (syntax uses the undefined ones here)
            if ( node.definedDimensions.length != node.numUndefinedDims )
                throw new EvalError(
                        "Incompatible initializer. Allocation calls for a " +
                                node.numUndefinedDims+ " dimensional array, but initializer is a " +
                                actualDimensions + " dimensional array", node, callstack );

            // fill in definedDimensions [] lengths
            Object arraySlice = initValue;
            for ( int i = 0; i < node.definedDimensions.length; i++ ) {
                node.definedDimensions[i] = Array.getLength(arraySlice);
                if ( node.definedDimensions[i] > 0 )
                    arraySlice = Array.get(arraySlice, 0);
            }

            return initValue;
        }
        else
        // Evaluate the defined dimensions of the array
        {
            node.definedDimensions = new int[ node.numDefinedDims ];

            for(int i = 0; i < node.numDefinedDims; i++)
            {
                try {
                    Object length = ((SimpleNode)node.jjtGetChild(i)).accept(this);
                    node.definedDimensions[i] = ((Primitive)length).intValue();
                }
                catch(Exception e)
                {
                    throw new EvalError(
                            "Array index: " + i +
                                    " does not evaluate to an integer", node, callstack );
                }
            }
        }

        return Primitive.VOID;
    }

    public Object evalArrayDimensions( BSHArrayDimensions node,
            Class type )
            throws EvalError
    {
        if ( Interpreter.DEBUG ) Interpreter.debug("array base type = "+type);
        node.baseType = type;
        return node.accept(this);
    }




    /**
     Construct the array from the initializer syntax.

     @param baseType the base class type of the array (no dimensionality)
     @param dimensions the top number of dimensions of the array
     e.g. 2 for a String [][];
     */
    public Object evalArrayInitializer(BSHArrayInitializer nodeA, Class baseType, int dimensions,
                        CallStack callstack, Interpreter interpreter )
            throws EvalError
    {
        int numInitializers = nodeA.jjtGetNumChildren();

        // allocate the array to store the initializers
        int [] dima = new int [dimensions]; // description of the array
        // The other dimensions default to zero and are assigned when
        // the values are set.
        dima[0] = numInitializers;
        Object initializers =  Array.newInstance( baseType, dima );

        // Evaluate the initializers
        for (int i = 0; i < numInitializers; i++)
        {
            SimpleNode childNode = (SimpleNode)nodeA.jjtGetChild(i);
            Object currentInitializer;
            if ( childNode instanceof BSHArrayInitializer ) {
                if ( dimensions < 2 )
                    throw new EvalError(
                            "Invalid Location for Intializer, position: "+i,
                            nodeA, callstack );
                currentInitializer =
                        evalArrayInitializer(((BSHArrayInitializer)childNode),
                                baseType, dimensions-1, callstack, interpreter);
            } else
                currentInitializer = childNode.accept(this);

            if ( currentInitializer == Primitive.VOID )
                throw new EvalError(
                        "Void in array initializer, position"+i, nodeA, callstack );

            // Determine if any conversion is necessary on the initializers.
            //
            // Quick test to see if conversions apply:
            // If the dimensionality of the array is 1 then the elements of
            // the initializer can be primitives or boxable types.  If it is
            // greater then the values must be array (object) types and there
            // are currently no conversions that we do on those.
            // If we have conversions on those in the future then we need to
            // get the real base type here instead of the dimensionless one.
            Object value = currentInitializer;
            if ( dimensions == 1 )
            {
                // We do a bsh cast here.  strictJava should be able to affect
                // the cast there when we tighten control
                try {
                    value = Types.castObject(
                            currentInitializer, baseType, Types.CAST );
                } catch ( UtilEvalError e ) {
                    throw e.toEvalError(
                            "Error in array initializer", nodeA, callstack );
                }
                // unwrap any primitive, map voids to null, etc.
                value = Primitive.unwrap( value );
            }

            // store the value in the array
            try {
                Array.set(initializers, i, value);
            } catch( IllegalArgumentException e ) {
                Interpreter.debug("illegal arg"+e);
                throwTypeError( nodeA, baseType, currentInitializer, i, callstack );
            } catch( ArrayStoreException e ) { // I think this can happen
                Interpreter.debug("arraystore"+e);
                throwTypeError(nodeA, baseType, currentInitializer, i, callstack );
            }
        }

        return initializers;
    }

    private void throwTypeError( BSHArrayInitializer node,
            Class baseType, Object initializer, int argNum, CallStack callstack )
            throws EvalError
    {
        String rhsType;
        if (initializer instanceof Primitive)
            rhsType =
                    ((Primitive)initializer).getType().getName();
        else
            rhsType = Reflect.normalizeClassName(
                    initializer.getClass());

        throw new EvalError ( "Incompatible type: " + rhsType
                +" in initializer of array type: "+ baseType
                +" at position: "+argNum, node, callstack );
    }


    @Override
    public Object visit(BSHArrayInitializer node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHArrayInitializer class.");
    }


    @Override
    public Object visit(BSHAssignment node) {
        BSHPrimaryExpression lhsNode =
                (BSHPrimaryExpression)node.jjtGetChild(0);

        if ( lhsNode == null )
            throw new InterpreterError( "Error, null LHSnode" );

        boolean strictJava = interpreter.getStrictJava();
        LHS lhs = primaryExprToLHS(lhsNode);
        if ( lhs == null )
            throw new InterpreterError( "Error, null LHS" );

        // For operator-assign operations save the lhs value before evaluating
        // the rhs.  This is correct Java behavior for postfix operations
        // e.g. i=1; i+=i++; // should be 2 not 3
        Object lhsValue = null;
        if ( node.operator != ParserConstants.ASSIGN ) // assign doesn't need the pre-value
            try {
                lhsValue = lhs.getValue();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( node, callstack );
            }

        SimpleNode rhsNode = (SimpleNode)node.jjtGetChild(1);

        Object rhs;

        // implement "blocks" foo = { };
        // if ( rhsNode instanceof BSHBlock )
        //    rsh =
        // else
        rhs = rhsNode.accept(this);

        if ( rhs == Primitive.VOID )
            throw new EvalError("Void assignment.", node, callstack );

        try {
            switch(node.operator)
            {
                case ParserConstants.ASSIGN:
                    return lhs.assign( rhs, strictJava );

                case ParserConstants.PLUSASSIGN:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.PLUS), strictJava );

                case ParserConstants.MINUSASSIGN:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.MINUS), strictJava );

                case ParserConstants.STARASSIGN:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.STAR), strictJava );

                case ParserConstants.SLASHASSIGN:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.SLASH), strictJava );

                case ParserConstants.ANDASSIGN:
                case ParserConstants.ANDASSIGNX:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.BIT_AND), strictJava );

                case ParserConstants.ORASSIGN:
                case ParserConstants.ORASSIGNX:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.BIT_OR), strictJava );

                case ParserConstants.XORASSIGN:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.XOR), strictJava );

                case ParserConstants.MODASSIGN:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.MOD), strictJava );

                case ParserConstants.LSHIFTASSIGN:
                case ParserConstants.LSHIFTASSIGNX:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.LSHIFT), strictJava );

                case ParserConstants.RSIGNEDSHIFTASSIGN:
                case ParserConstants.RSIGNEDSHIFTASSIGNX:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.RSIGNEDSHIFT ), strictJava );

                case ParserConstants.RUNSIGNEDSHIFTASSIGN:
                case ParserConstants.RUNSIGNEDSHIFTASSIGNX:
                    return lhs.assign(
                            BshInterpreterUtil.operation(node, lhsValue, rhs, ParserConstants.RUNSIGNEDSHIFT),
                            strictJava );

                default:
                    throw new InterpreterError(
                            "unimplemented operator in assignment BSH");
            }
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }


    @Override
    public Object visit(BSHBinaryExpression node) {
        Object lhs = ((SimpleNode)node.jjtGetChild(0)).accept(this);

        /*
              Doing instanceof?  Next node is a type.
          */
        if (node.kind == ParserConstants.INSTANCEOF)
        {
            // null object ref is not instance of any type
            if ( lhs == Primitive.NULL )
                return new Primitive(false);

            Class rhs = getType(((BSHType)node.jjtGetChild(1)));
            /*
               // primitive (number or void) cannot be tested for instanceof
               if (lhs instanceof Primitive)
                   throw new EvalError("Cannot be instance of primitive type." );
           */
            /*
                   Primitive (number or void) is not normally an instanceof
                   anything.  But for internal use we'll test true for the
                   bsh.Primitive class.
                   i.e. (5 instanceof bsh.Primitive) will be true
               */
            if ( lhs instanceof Primitive )
                if ( rhs == bsh.Primitive.class )
                    return new Primitive(true);
                else
                    return new Primitive(false);

            // General case - performe the instanceof based on assignability
            boolean ret = Types.isJavaBaseAssignable( rhs, lhs.getClass() );
            return new Primitive(ret);
        }


        // The following two boolean checks were tacked on.
        // This could probably be smoothed out.

        /*
              Look ahead and short circuit evaluation of the rhs if:
                  we're a boolean AND and the lhs is false.
          */
        if ( node.kind == ParserConstants.BOOL_AND || node.kind == ParserConstants.BOOL_ANDX ) {
            Object obj = lhs;
            if ( node.isPrimitiveValue(lhs) )
                obj = ((Primitive)lhs).getValue();
            if ( obj instanceof Boolean &&
                    ( ((Boolean)obj).booleanValue() == false ) )
                return new Primitive(false);
        }
        /*
              Look ahead and short circuit evaluation of the rhs if:
                  we're a boolean AND and the lhs is false.
          */
        if ( node.kind == ParserConstants.BOOL_OR || node.kind == ParserConstants.BOOL_ORX ) {
            Object obj = lhs;
            if ( node.isPrimitiveValue(lhs) )
                obj = ((Primitive)lhs).getValue();
            if ( obj instanceof Boolean &&
                    ( ((Boolean)obj).booleanValue() == true ) )
                return new Primitive(true);
        }

        // end stuff that was tacked on for boolean short-circuiting.

        /*
              Are both the lhs and rhs either wrappers or primitive values?
              do binary op
          */
        boolean isLhsWrapper = node.isWrapper(lhs);
        Object rhs = ((SimpleNode)node.jjtGetChild(1)).accept(this); //eval(callstack, interpreter);
        boolean isRhsWrapper = node.isWrapper(rhs);
        if (
                ( isLhsWrapper || node.isPrimitiveValue(lhs) )
                        && ( isRhsWrapper || node.isPrimitiveValue(rhs) )
                )
        {
            // Special case for EQ on two wrapper objects
            if ( (isLhsWrapper && isRhsWrapper && node.kind == ParserConstants.EQ))
            {
                /*
                        Don't auto-unwrap wrappers (preserve identity semantics)
                        FALL THROUGH TO OBJECT OPERATIONS BELOW.
                    */
            } else
                try {
                    return Primitive.binaryOperation(lhs, rhs, node.kind);
                } catch ( UtilEvalError e ) {
                    throw e.toEvalError( node, callstack  );
                }
        }
        /*
      Doing the following makes it hard to use untyped vars...
      e.g. if ( arg == null ) ...what if arg is a primitive?
      The answer is that we should test only if the var is typed...?
      need to get that info here...

          else
          {
          // Do we have a mixture of primitive values and non-primitives ?
          // (primitiveValue = not null, not void)

          int primCount = 0;
          if ( isPrimitiveValue( lhs ) )
              ++primCount;
          if ( isPrimitiveValue( rhs ) )
              ++primCount;

          if ( primCount > 1 )
              // both primitive types, should have been handled above
              throw new InterpreterError("should not be here");
          else
          if ( primCount == 1 )
              // mixture of one and the other
              throw new EvalError("Operator: '" + tokenImage[kind]
                  +"' inappropriate for object / primitive combination.",
                  this, callstack );

          // else fall through to handle both non-primitive types

          // end check for primitive and non-primitive mix
          }
      */

        /*
              Treat lhs and rhs as arbitrary objects and do the operation.
              (including NULL and VOID represented by their Primitive types)
          */
        //System.out.println("binary op arbitrary obj: {"+lhs+"}, {"+rhs+"}");
        switch(node.kind)
        {
            case ParserConstants.EQ:
                return new Primitive((lhs == rhs));

            case ParserConstants.NE:
                return new Primitive((lhs != rhs));

            case ParserConstants.PLUS:
                if(lhs instanceof String || rhs instanceof String)
                    return lhs.toString() + rhs.toString();

                // FALL THROUGH TO DEFAULT CASE!!!

            default:
                if(lhs instanceof Primitive || rhs instanceof Primitive)
                    if ( lhs == Primitive.VOID || rhs == Primitive.VOID )
                        throw new EvalError(
                                "illegal use of undefined variable, class, or 'void' literal",
                                node, callstack );
                    else
                    if ( lhs == Primitive.NULL || rhs == Primitive.NULL )
                        throw new EvalError(
                                "illegal use of null value or 'null' literal", node, callstack);

                throw new EvalError("Operator: '" + ParserConstants.tokenImage[node.kind] +
                        "' inappropriate for objects", node, callstack );
        }
    }


    /**
     @param overrideNamespace if set to true the block will be executed
     in the current namespace (not a subordinate one).
     <p>
     If true *no* new BlockNamespace will be swapped onto the stack and
     the eval will happen in the current
     top namespace.  This is used by BshMethod, TryStatement, etc.
     which must intialize the block first and also for those that perform
     multiple passes in the same block.
     */
    public Object evalBlock( BSHBlock node,
            boolean overrideNamespace )
            throws EvalError
    {
        Object syncValue = null;
        if ( node.isSynchronized )
        {
            // First node is the expression on which to sync
            SimpleNode exp = ((SimpleNode)node.jjtGetChild(0));
            syncValue = exp.accept(this);
        }

        Object ret;
        if ( node.isSynchronized ) // Do the actual synchronization
            synchronized( syncValue )
            {
                ret = evalBlock(node, overrideNamespace, null/*filter*/);
            }
        else
            ret = evalBlock(node, overrideNamespace, null/*filter*/ );

        return ret;
    }

    public Object evalBlock( BSHBlock block,
            boolean overrideNamespace, BSHBlock.NodeFilter nodeFilter )
            throws EvalError
    {
        Object ret = Primitive.VOID;
        NameSpace enclosingNameSpace = null;
        if ( !overrideNamespace )
        {
            enclosingNameSpace= callstack.top();
            BlockNameSpace bodyNameSpace =
                    new BlockNameSpace( enclosingNameSpace );

            callstack.swap( bodyNameSpace );
        }

        int startChild = block.isSynchronized ? 1 : 0;
        int numChildren = block.jjtGetNumChildren();

        try {
            /*
                   Evaluate block in two passes:
                   First do class declarations then do everything else.
               */
            for(int i=startChild; i<numChildren; i++)
            {
                SimpleNode node = ((SimpleNode)block.jjtGetChild(i));

                if ( nodeFilter != null && !nodeFilter.isVisible( node ) )
                    continue;

                if ( node instanceof BSHClassDeclaration )
                    node.accept(this);
            }
            for(int i=startChild; i<numChildren; i++)
            {
                SimpleNode node = ((SimpleNode)block.jjtGetChild(i));
                if ( node instanceof BSHClassDeclaration )
                    continue;

                // filter nodes
                if ( nodeFilter != null && !nodeFilter.isVisible( node ) )
                    continue;

                ret = node.accept(this);

                // statement or embedded block evaluated a return statement
                if ( ret instanceof ReturnControl )
                    break;
            }
        } finally {
            // make sure we put the namespace back when we leave.
            if ( !overrideNamespace )
                callstack.swap( enclosingNameSpace );
        }
        return ret;
    }


    @Override
    public Object visit(BSHBlock node) {
        return evalBlock(node, false);
    }


    @Override
    public Object visit(BSHCastExpression node) {
        NameSpace namespace = callstack.top();
        Class toType = getType(((BSHType)node.jjtGetChild(0)));
        SimpleNode expression = (SimpleNode)node.jjtGetChild(1);

        // evaluate the expression
        Object fromValue = expression.accept(this);
        Class fromType = fromValue.getClass();

        // TODO: need to add isJavaCastable() test for strictJava
        // (as opposed to isJavaAssignable())
        try {
            return Types.castObject( fromValue, toType, Types.CAST );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack  );
        }
    }


    @Override
    public Object visit(BSHClassDeclaration node) {
        synchronized (node) {
            if (node.generatedClass == null) {
                node.generatedClass = generateClass(node);
            }
            return node.generatedClass;
        }
    }

    public Class<?> generateClass(BSHClassDeclaration node) throws EvalError {
        int child = 0;

        // resolve superclass if any
        Class superClass = null;
        if ( node.extend ) {
            BSHAmbiguousName superNode = (BSHAmbiguousName)node.jjtGetChild(child++);
            superClass = ambiguousNameToClass( superNode );
        }

        // Get interfaces
        Class [] interfaces = new Class[node.numInterfaces];
        for( int i=0; i<node.numInterfaces; i++) {
            BSHAmbiguousName node1 = (BSHAmbiguousName)node.jjtGetChild(child++);
            interfaces[i] = ambiguousNameToClass(node1);
            if ( !interfaces[i].isInterface() )
                throw new EvalError(
                        "Type: "+node1.text+" is not an interface!",
                        node, callstack );
        }

        BSHBlock block;
        // Get the class body BSHBlock
        if ( child < node.jjtGetNumChildren() )
            block = (BSHBlock) node.jjtGetChild(child);
        else
            block = new BSHBlock( ParserTreeConstants.JJTBLOCK );

        return ClassGenerator.getClassGenerator().generateClass(
                node.name, node.modifiers, interfaces, superClass, block, node.isInterface,
                callstack, interpreter );
    }



    @Override
    public Object visit(BSHEnhancedForStatement node) {
        Class elementType = null;
        SimpleNode expression, statement=null;

        NameSpace enclosingNameSpace = callstack.top();
        SimpleNode firstNode =((SimpleNode)node.jjtGetChild(0));
        int nodeCount = node.jjtGetNumChildren();

        if ( firstNode instanceof BSHType )
        {
            elementType=getType(((BSHType)firstNode));
            expression=((SimpleNode)node.jjtGetChild(1));
            if ( nodeCount>2 )
                statement=((SimpleNode)node.jjtGetChild(2));
        } else
        {
            expression=firstNode;
            if ( nodeCount>1 )
                statement=((SimpleNode)node.jjtGetChild(1));
        }

        BlockNameSpace eachNameSpace = new BlockNameSpace( enclosingNameSpace );
        callstack.swap( eachNameSpace );

        final Object iteratee = expression.accept(this);

        if ( iteratee == Primitive.NULL )
            throw new EvalError("The collection, array, map, iterator, or " +
                    "enumeration portion of a for statement cannot be null.",
                    node, callstack );

        CollectionManager cm = CollectionManager.getCollectionManager();
        if ( !cm.isBshIterable( iteratee ) )
            throw new EvalError("Can't iterate over type: "
                    +iteratee.getClass(), node, callstack );
        Iterator iterator = cm.getBshIterator( iteratee );

        Object returnControl = Primitive.VOID;
        while( iterator.hasNext() )
        {
            try {
                Object value = iterator.next();
                if ( value == null )
                    value = Primitive.NULL;
                if ( elementType != null )
                    eachNameSpace.setTypedVariable(
                            node.varName/*name*/, elementType/*type*/,
                            value, new Modifiers()/*none*/ );
                else
                    eachNameSpace.setVariable( node.varName, value, false );
            } catch ( UtilEvalError e ) {
                throw e.toEvalError(
                        "for loop iterator variable:"+ node.varName, node, callstack );
            }

            boolean breakout = false; // switch eats a multi-level break here?
            if ( statement != null ) // not empty statement
            {
                Object ret = statement.accept(this); //eval( callstack, interpreter );

                if (ret instanceof ReturnControl)
                {
                    switch(((ReturnControl)ret).kind)
                    {
                        case ParserConstants.RETURN:
                            returnControl = ret;
                            breakout = true;
                            break;

                        case ParserConstants.CONTINUE:
                            break;

                        case ParserConstants.BREAK:
                            breakout = true;
                            break;
                    }
                }
            }

            if (breakout)
                break;
        }

        callstack.swap(enclosingNameSpace);
        return returnControl;
    }


    @Override
    public Object visit(BSHFormalComment node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHFormalComment class.");
    }


    @Override
    public Object visit(BSHFormalParameter node) {
        if ( node.jjtGetNumChildren() > 0 )
            node.type = getType(((BSHType)node.jjtGetChild(0)));
        else
            node.type = node.UNTYPED;

        return node.type;
    }

    public String getFormalParameterTypeDescriptor( BSHFormalParameter node,
             String defaultPackage )
    {
        if ( node.jjtGetNumChildren() > 0 )
            return getTypeDescriptor(((BSHType) node.jjtGetChild(0)), defaultPackage);
        else
            // this will probably not get used
            return "Ljava/lang/Object;";  // Object type
    }



    @Override
    public Object visit(BSHFormalParameters node) {
        if ( node.paramTypes != null )
            return node.paramTypes;

        node.insureParsed();
        Class [] paramTypes = new Class[node.numArgs];

        for(int i=0; i<node.numArgs; i++)
        {
            BSHFormalParameter param = (BSHFormalParameter)node.jjtGetChild(i);
            paramTypes[i] = (Class)param.accept(this); //eval( callstack, interpreter );
        }

        node.paramTypes = paramTypes;

        return paramTypes;
    }

    public String [] getTypeDescriptors( BSHFormalParameters node, String defaultPackage )
    {
        if ( node.typeDescriptors != null )
            return node.typeDescriptors;

        node.insureParsed();
        String [] typeDesc = new String[node.numArgs];

        for(int i=0; i<node.numArgs; i++)
        {
            BSHFormalParameter param = (BSHFormalParameter)node.jjtGetChild(i);
            typeDesc[i] = getFormalParameterTypeDescriptor(param, defaultPackage );
        }

        node.typeDescriptors = typeDesc;
        return typeDesc;
    }


    @Override
    public Object visit(BSHForStatement node) {
        int i = 0;
        if(node.hasForInit)
            node.forInit = ((SimpleNode)node.jjtGetChild(i++));
        if(node.hasExpression)
            node.expression = ((SimpleNode)node.jjtGetChild(i++));
        if(node.hasForUpdate)
            node.forUpdate = ((SimpleNode)node.jjtGetChild(i++));
        if(i < node.jjtGetNumChildren()) // should normally be
            node.statement = ((SimpleNode)node.jjtGetChild(i));

        NameSpace enclosingNameSpace= callstack.top();
        BlockNameSpace forNameSpace = new BlockNameSpace( enclosingNameSpace );

        /*
              Note: some interesting things are going on here.

              1) We swap instead of push...  The primary mode of operation
              acts like we are in the enclosing namespace...  (super must be
              preserved, etc.)

              2) We do *not* call the body block eval with the namespace
              override.  Instead we allow it to create a second subordinate
              BlockNameSpace child of the forNameSpace.  Variable propogation
              still works through the chain, but the block's child cleans the
              state between iteration.
              (which is correct Java behavior... see forscope4.bsh)
          */

        // put forNameSpace it on the top of the stack
        // Note: it's important that there is only one exit point from this
        // method so that we can swap back the namespace.
        callstack.swap( forNameSpace );

        // Do the for init
        if ( node.hasForInit )
            node.forInit.accept(this);

        Object returnControl = Primitive.VOID;
        while(true)
        {
            if ( node.hasExpression )
            {
                boolean cond = BshInterpreterUtil.evaluateCondition(
                        node.expression, this);

                if ( !cond )
                    break;
            }

            boolean breakout = false; // switch eats a multi-level break here?
            if ( node.statement != null ) // not empty statement
            {
                // do *not* invoke special override for block... (see above)
                Object ret = node.statement.accept(this);

                if (ret instanceof ReturnControl)
                {
                    switch(((ReturnControl)ret).kind)
                    {
                        case ParserConstants.RETURN:
                            returnControl = ret;
                            breakout = true;
                            break;

                        case ParserConstants.CONTINUE:
                            break;

                        case ParserConstants.BREAK:
                            breakout = true;
                            break;
                    }
                }
            }

            if ( breakout )
                break;

            if ( node.hasForUpdate )
                node.forUpdate.accept(this);
        }

        callstack.swap( enclosingNameSpace );  // put it back
        return returnControl;
    }


    @Override
    public Object visit(BSHIfStatement node) {
        Object ret = null;

        if( BshInterpreterUtil.evaluateCondition(
                (SimpleNode) node.jjtGetChild(0), this) )
            ret = ((SimpleNode)node.jjtGetChild(1)).accept(this);
        else
        if(node.jjtGetNumChildren() > 2)
            ret = ((SimpleNode)node.jjtGetChild(2)).accept(this);

        if(ret instanceof ReturnControl)
            return ret;
        else
            return Primitive.VOID;
    }


    @Override
    public Object visit(BSHImportDeclaration node) {
        NameSpace namespace = callstack.top();
        if ( node.superImport )
            try {
                namespace.doSuperImport();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( node, callstack  );
            }
        else
        {
            if ( node.staticImport )
            {
                if ( node.importPackage )
                {
                    Class clas = ambiguousNameToClass(((BSHAmbiguousName)node.jjtGetChild(0)));
                    namespace.importStatic( clas );
                } else
                    throw new EvalError(
                            "static field imports not supported yet",
                            node, callstack );
            } else
            {
                String name = ((BSHAmbiguousName)node.jjtGetChild(0)).text;
                if ( node.importPackage )
                    namespace.importPackage(name);
                else
                    namespace.importClass(name);
            }
        }

        return Primitive.VOID;
    }


    @Override
    public Object visit(BSHLiteral node) {
        if (node.value == null)
            throw new InterpreterError("Null in bsh literal: "+node.value);

        return node.value;
    }


    @Override
    public Object visit(BSHMethodDeclaration node) {
        node.returnType = evalMethodReturnType(node);
        evalNodes(node);

        // Install an *instance* of this method in the namespace.
        // See notes in BshMethod

        // This is not good...
        // need a way to update eval without re-installing...
        // so that we can re-eval params, etc. when classloader changes
        // look into this

        NameSpace namespace = callstack.top();
        BshMethod bshMethod = new BshMethod( node, namespace, node.modifiers );
        try {
            namespace.setMethod( bshMethod );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError(node,callstack);
        }

        return Primitive.VOID;
    }


    public Class evalReturnType( BSHReturnType node ) throws EvalError
    {
        if ( node.isVoid )
            return Void.TYPE;
        else
            return getType(getTypeNode(node));
    }


    public BSHType getTypeNode(BSHReturnType node) {
        return (BSHType)node.jjtGetChild(0);
    }

    public String getTypeDescriptor( BSHReturnType node,
            String defaultPackage )
    {
        if ( node.isVoid )
            return "V";
        else
            return getTypeDescriptor(getTypeNode(node),
                    defaultPackage);
    }


    public String getReturnTypeDescriptor(BSHMethodDeclaration node,
            String defaultPackage )
    {
        node.insureNodesParsed();
        if ( node.returnTypeNode == null )
            return null;
        else
            return getTypeDescriptor(node.returnTypeNode, defaultPackage );
    }

    public BSHReturnType getReturnTypeNode(BSHMethodDeclaration node) {
        node.insureNodesParsed();
        return node.returnTypeNode;
    }

    /**
     Evaluate the return type node.
     @return the type or null indicating loosely typed return
     */
    public Class evalMethodReturnType( BSHMethodDeclaration node )
            throws EvalError
    {
        node.insureNodesParsed();
        if ( node.returnTypeNode != null )
            return evalReturnType(node.returnTypeNode);
        else
            return null;
    }


    public void evalNodes(BSHMethodDeclaration node)
            throws EvalError
    {
        node.insureNodesParsed();

        // validate that the throws names are class names
        for(int i=node.firstThrowsClause; i<node.numThrows+node.firstThrowsClause; i++)
            ambiguousNameToClass(((BSHAmbiguousName)node.jjtGetChild(i)) );

        node.paramsNode.accept(this);

        // if strictJava mode, check for loose parameters and return type
        if ( interpreter.getStrictJava() )
        {
            for(int i=0; i<node.paramsNode.paramTypes.length; i++)
                if ( node.paramsNode.paramTypes[i] == null )
                    // Warning: Null callstack here.  Don't think we need
                    // a stack trace to indicate how we sourced the method.
                    throw new EvalError(
                            "(Strict Java Mode) Undeclared argument type, parameter: " +
                                    node.paramsNode.getParamNames()[i] + " in method: "
                                    + node.name, node, null );

            if ( node.returnType == null )
                // Warning: Null callstack here.  Don't think we need
                // a stack trace to indicate how we sourced the method.
                throw new EvalError(
                        "(Strict Java Mode) Undeclared return type for method: "
                                + node.name, node, null );
        }
    }


    @Override
    public Object visit(BSHMethodInvocation node) {
        NameSpace namespace = callstack.top();
        BSHAmbiguousName nameNode = node.getNameNode();

        // Do not evaluate methods this() or super() in class instance space
        // (i.e. inside a constructor)
        if ( namespace.getParent() != null && namespace.getParent().isClass
                && ( nameNode.text.equals("super") || nameNode.text.equals("this") )
                )
            return Primitive.VOID;

        Name name = nameNode.getName(namespace);
        Object[] args = getArguments(node.getArgsNode());

        // This try/catch block is replicated is BSHPrimarySuffix... need to
        // factor out common functionality...
        // Move to Reflect?
        try {
            return name.invokeMethod( this, args, node);
        } catch ( ReflectError e ) {
            throw new EvalError(
                    "Error in method invocation: " + e.getMessage(),
                    node, callstack, e );
        } catch ( InvocationTargetException e )
        {
            String msg = "Method Invocation "+name;
            Throwable te = e.getTargetException();

            /*
                   Try to squeltch the native code stack trace if the exception
                   was caused by a reflective call back into the bsh interpreter
                   (e.g. eval() or source()
               */
            boolean isNative = true;
            if ( te instanceof EvalError )
                if ( te instanceof TargetError )
                    isNative = ((TargetError)te).inNativeCode();
                else
                    isNative = false;

            throw new TargetError( msg, te, node, callstack, isNative );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }


    @Override
    public Object visit(BSHPackageDeclaration node) {
        BSHAmbiguousName name = (BSHAmbiguousName)node.jjtGetChild(0);
        NameSpace namespace = callstack.top();
        namespace.setPackage( name.text );
        // import the package we're in by default...
        namespace.importPackage( name.text );
        return Primitive.VOID;
    }


    /**
     Evaluate to a value object.
     */
    private LHS primaryExprToLHS(BSHPrimaryExpression node)
            throws EvalError
    {
        Object obj = evalPrimaryExpr(node, true);

        if ( ! (obj instanceof LHS) )
            throw new EvalError("Can't assign to:", node, callstack );
        else
            return (LHS)obj;
    }


    /*
         Our children are a prefix expression and any number of suffixes.
         <p>

         We don't eval() any nodes until the suffixes have had an
         opportunity to work through them.  This lets the suffixes decide
         how to interpret an ambiguous name (e.g. for the .class operation).
     */
    private Object evalPrimaryExpr( BSHPrimaryExpression node, boolean toLHS )
            throws EvalError
    {
        //CallStack callstack = visitor.getCallstack();
        //Interpreter interpreter = visitor.getInterpreter();

        Object obj = node.jjtGetChild(0);
        int numChildren = node.jjtGetNumChildren();

        for(int i=1; i<numChildren; i++)
            obj = doSuffix(((BSHPrimarySuffix)node.jjtGetChild(i)), obj, toLHS);

        /*
              If the result is a Node eval() it to an object or LHS
              (as determined by toLHS)
          */
        if ( obj instanceof SimpleNode )
            if ( obj instanceof BSHAmbiguousName)
                if ( toLHS )
                    obj = ambiguousNameToLHS(((BSHAmbiguousName)obj));
                else
                    obj = ambiguousNameToObject(((BSHAmbiguousName)obj));
            else
                // Some arbitrary kind of node
                if ( toLHS )
                    // is this right?
                    throw new EvalError("Can't assign to prefix.",
                            node, callstack );
                else
                    obj = ((SimpleNode)obj).accept(this);

        // return LHS or value object as determined by toLHS
        if ( obj instanceof LHS )
            if ( toLHS )
                return obj;
            else
                try {
                    return ((LHS)obj).getValue();
                } catch ( UtilEvalError e ) {
                    throw e.toEvalError( node, callstack );
                }
        else
            return obj;
    }


    @Override
    public Object visit(BSHPrimaryExpression node) {
        return evalPrimaryExpr(node, false);
    }


    private Object doSuffix( BSHPrimarySuffix node,
            Object obj, boolean toLHS)
            throws EvalError
    {
        // Handle ".class" suffix operation
        // Prefix must be a BSHType
        if ( node.operation == BSHPrimarySuffix.CLASS )
            if ( obj instanceof BSHType) {
                if ( toLHS )
                    throw new EvalError("Can't assign .class",
                            node, callstack );
                NameSpace namespace = callstack.top();
                return getType(((BSHType)obj));
            } else
                throw new EvalError(
                        "Attempt to use .class suffix on non class.",
                        node, callstack );

        /*
              Evaluate our prefix if it needs evaluating first.
              If this is the first evaluation our prefix mayb be a Node
              (directly from the PrimaryPrefix) - eval() it to an object.
              If it's an LHS, resolve to a value.

              Note: The ambiguous name construct is now necessary where the node
              may be an ambiguous name.  If this becomes common we might want to
              make a static method nodeToObject() or something.  The point is
              that we can't just eval() - we need to direct the evaluation to
              the context sensitive type of result; namely object, class, etc.
          */
        if ( obj instanceof SimpleNode )
            if ( obj instanceof BSHAmbiguousName)
                obj = ambiguousNameToObject(((BSHAmbiguousName)obj));
            else
                obj = ((SimpleNode)obj).accept(this);
        else
        if ( obj instanceof LHS )
            try {
                obj = ((LHS)obj).getValue();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( node, callstack );
            }

        try
        {
            switch(node.operation)
            {
                case BSHPrimarySuffix.INDEX:
                    return doIndex(node, obj, toLHS );

                case BSHPrimarySuffix.NAME:
                    return doName(node, obj, toLHS );

                case BSHPrimarySuffix.PROPERTY:
                    return doProperty( node, toLHS, obj);

                default:
                    throw new InterpreterError( "Unknown suffix type" );
            }
        }
        catch(ReflectError e)
        {
            throw new EvalError("reflection error: " + e, node, callstack, e );
        }
        catch(InvocationTargetException e)
        {
            throw new TargetError( "target exception", e.getTargetException(),
                    node, callstack, true);
        }
    }

    /*
         Field access, .length on array, or a method invocation
         Must handle toLHS case for each.
     */
    private Object doName( BSHPrimarySuffix node, Object obj, boolean toLHS)
            throws EvalError, ReflectError, InvocationTargetException
    {
        try {
            // .length on array
            if ( node.field.equals("length") && obj.getClass().isArray() )
                if ( toLHS )
                    throw new EvalError(
                            "Can't assign array length", node, callstack );
                else
                    return new Primitive(Array.getLength(obj));

            // field access
            if ( node.jjtGetNumChildren() == 0 )
                if ( toLHS )
                    return Reflect.getLHSObjectField(obj, node.field);
                else
                    return Reflect.getObjectFieldValue( obj, node.field );

            // Method invocation
            // (LHS or non LHS evaluation can both encounter method calls)
            Object[] oa = getArguments(((BSHArguments)node.jjtGetChild(0)));

            // TODO:
            // Note: this try/catch block is copied from BSHMethodInvocation
            // we need to factor out this common functionality and make sure
            // we handle all cases ... (e.g. property style access, etc.)
            // maybe move this to Reflect ?
            try {
                return Reflect.invokeObjectMethod(
                        obj, node.field, oa, this, node );
            } catch ( ReflectError e ) {
                throw new EvalError(
                        "Error in method invocation: " + e.getMessage(),
                        node, callstack, e );
            } catch ( InvocationTargetException e )
            {
                String msg = "Method Invocation "+node.field;
                Throwable te = e.getTargetException();

                /*
                        Try to squeltch the native code stack trace if the exception
                        was caused by a reflective call back into the bsh interpreter
                        (e.g. eval() or source()
                    */
                boolean isNative = true;
                if ( te instanceof EvalError )
                    if ( te instanceof TargetError )
                        isNative = ((TargetError)te).inNativeCode();
                    else
                        isNative = false;

                throw new TargetError( msg, te, node, callstack, isNative );
            }

        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }


    /**
     array index.
     Must handle toLHS case.
     */
    private Object doIndex( BSHPrimarySuffix node,
            Object obj, boolean toLHS )
            throws EvalError, ReflectError
    {
        int index = BshInterpreterUtil.getIndexAux( obj, this, node );
        if ( toLHS )
            return new LHS(obj, index);
        else
            try {
                return Reflect.getIndex(obj, index);
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( node, callstack );
            }
    }


    /**
     Property access.
     Must handle toLHS case.
     */
    private Object doProperty( BSHPrimarySuffix node, boolean toLHS, Object obj )
            throws EvalError
    {
        if(obj == Primitive.VOID)
            throw new EvalError(
                    "Attempt to access property on undefined variable or class name",
                    node, callstack );

        if ( obj instanceof Primitive )
            throw new EvalError("Attempt to access property on a primitive",
                    node, callstack );

        Object value = ((SimpleNode)node.jjtGetChild(0)).accept(this);

        if ( !( value instanceof String ) )
            throw new EvalError(
                    "Property expression must be a String or identifier.",
                    node, callstack );

        if ( toLHS )
            return new LHS(obj, (String)value);

        // Property style access to Hashtable or Map
        CollectionManager cm = CollectionManager.getCollectionManager();
        if ( cm.isMap( obj ) )
        {
            Object val = cm.getFromMap( obj, value/*key*/ );
            return ( val == null ?  val = Primitive.NULL : val );
        }

        try {
            return Reflect.getObjectProperty( obj, (String)value );
        }
        catch ( UtilEvalError e)
        {
            throw e.toEvalError( "Property: "+value, node, callstack );
        }
        catch (ReflectError e)
        {
            throw new EvalError("No such property: " + value, node, callstack );
        }
    }


    @Override
    public Object visit(BSHPrimarySuffix node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHPrimarySuffix class.", node);
    }


    @Override
    public Object visit(BSHPrimitiveType node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHPrimitiveType class.", node);
    }


    @Override
    public Object visit(BSHReturnStatement node) {
        Object value;
        if(node.jjtGetNumChildren() > 0)
            value = ((SimpleNode)node.jjtGetChild(0)).accept(this);
        else
            value = Primitive.VOID;

        return new ReturnControl( node.kind, value, node );
    }


    @Override
    public Object visit(BSHReturnType node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHPrimarySuffix class.");
    }


    @Override
    public Object visit(BSHStatementExpressionList node) {
        int n = node.jjtGetNumChildren();
        for(int i=0; i<n; i++)
        {
            SimpleNode nn = ((SimpleNode)node.jjtGetChild(i));
            nn.accept(this);
        }
        return Primitive.VOID;
    }


    @Override
    public Object visit(BSHSwitchLabel node) {
        if ( node.isDefault )
            return null; // should probably error
        SimpleNode label = ((SimpleNode)node.jjtGetChild(0));
        return label.accept(this);
    }


    @Override
    public Object visit(BSHSwitchStatement node) {
        int numchild = node.jjtGetNumChildren();
        int child = 0;
        SimpleNode switchExp = ((SimpleNode)node.jjtGetChild(child++));
        Object switchVal = switchExp.accept(this);

        /*
              Note: this could be made clearer by adding an inner class for the
              cases and an object context for the child traversal.
          */
        // first label
        BSHSwitchLabel label;
        Object obj;
        ReturnControl returnControl=null;

        // get the first label
        if ( child >= numchild )
            throw new EvalError("Empty switch statement.", node, callstack );
        label = ((BSHSwitchLabel)node.jjtGetChild(child++));

        // while more labels or blocks and haven't hit return control
        while ( child < numchild && returnControl == null )
        {
            // if label is default or equals switchVal
            if ( label.isDefault
                    || primitiveEquals(node, switchVal, label.accept(this), switchExp) )  {
                // execute nodes, skipping labels, until a break or return
                while ( child < numchild )
                {
                    obj = node.jjtGetChild(child++);
                    if ( obj instanceof BSHSwitchLabel )
                        continue;
                    // eval it
                    Object value =
                            ((SimpleNode)obj).accept(this);

                    // should check to disallow continue here?
                    if ( value instanceof ReturnControl ) {
                        returnControl = (ReturnControl)value;
                        break;
                    }
                }
            } else
            {
                // skip nodes until next label
                while ( child < numchild )
                {
                    obj = node.jjtGetChild(child++);
                    if ( obj instanceof BSHSwitchLabel ) {
                        label = (BSHSwitchLabel)obj;
                        break;
                    }
                }
            }
        }

        if ( returnControl != null && returnControl.kind == ParserConstants.RETURN )
            return returnControl;
        else
            return Primitive.VOID;
    }

    /**
     Helper method for testing equals on two primitive or boxable objects.
     yuck: factor this out into Primitive.java
     */
    public boolean primitiveEquals( BSHSwitchStatement node,
            Object switchVal, Object targetVal,
            SimpleNode switchExp  )
            throws EvalError
    {
        if ( switchVal instanceof Primitive || targetVal instanceof Primitive )
            try {
                // binaryOperation can return Primitive or wrapper type
                Object result = Primitive.binaryOperation(
                        switchVal, targetVal, ParserConstants.EQ );
                result = Primitive.unwrap( result );
                return result.equals( Boolean.TRUE );
            } catch ( UtilEvalError e ) {
                throw e.toEvalError(
                        "Switch value: "+switchExp.getText()+": ",
                        node, callstack );
            }
        else
            return switchVal.equals( targetVal );
    }

    @Override
    public Object visit(BSHTernaryExpression node) {
        SimpleNode
                cond = (SimpleNode)node.jjtGetChild(0),
                evalTrue = (SimpleNode)node.jjtGetChild(1),
                evalFalse = (SimpleNode)node.jjtGetChild(2);

        if ( BshInterpreterUtil.evaluateCondition(cond, this) )
            return evalTrue.accept(this);
        else
            return evalFalse.accept(this);
    }


    @Override
    public Object visit(BSHThrowStatement node) {
        Object obj = ((SimpleNode)node.jjtGetChild(0)).accept(this);

        // need to loosen this to any throwable... do we need to handle
        // that in interpreter somewhere?  check first...
        if(!(obj instanceof Exception))
            throw new EvalError("Expression in 'throw' must be Exception type",
                    node, callstack );

        // wrap the exception in a TargetException to propogate it up
        throw new TargetError( (Exception)obj, node, callstack );
    }


    @Override
    public Object visit(BSHTryStatement node) {
        BSHBlock tryBlock = ((BSHBlock)node.jjtGetChild(0));

        List<BSHFormalParameter> catchParams = new ArrayList<BSHFormalParameter>();
        List<BSHBlock> catchBlocks = new ArrayList<BSHBlock>();

        int nchild = node.jjtGetNumChildren();
        Node nodeObj = null;
        int i=1;
        while((i < nchild) && ((nodeObj = node.jjtGetChild(i++)) instanceof BSHFormalParameter))
        {
            catchParams.add((BSHFormalParameter)nodeObj);
            catchBlocks.add((BSHBlock)node.jjtGetChild(i++));
            nodeObj = null;
        }
        // finaly block
        BSHBlock finallyBlock = null;
        if(nodeObj != null)
            finallyBlock = (BSHBlock)nodeObj;

        // Why both of these?

        TargetError target = null;
        Throwable thrown = null;
        Object ret = null;

        /*
              Evaluate the contents of the try { } block and catch any resulting
              TargetErrors generated by the script.
              We save the callstack depth and if an exception is thrown we pop
              back to that depth before contiuing.  The exception short circuited
              any intervening method context pops.

              Note: we the stack info... what do we do with it?  append
              to exception message?
          */
        int callstackDepth = callstack.depth();
        try {
            ret = tryBlock.accept(this);
        }
        catch( TargetError e ) {
            target = e;
            String stackInfo = "Bsh Stack: ";
            while ( callstack.depth() > callstackDepth )
                stackInfo += "\t" + callstack.pop() +"\n";
        }

        // unwrap the target error
        if ( target != null )
            thrown = target.getTarget();


        // If we have an exception, find a catch
        if (thrown != null)
        {
            int n = catchParams.size();
            for(i=0; i<n; i++)
            {
                // Get catch block
                BSHFormalParameter fp = catchParams.get(i);

                // Should cache this subject to classloader change message
                // Evaluation of the formal parameter simply resolves its
                // type via the specified namespace.. it doesn't modify the
                // namespace.
                fp.accept(this);

                if ( fp.type == null && interpreter.getStrictJava() )
                    throw new EvalError(
                            "(Strict Java) Untyped catch block", node, callstack );

                // If the param is typed check assignability
                if ( fp.type != null )
                    try {
                        thrown = (Throwable)Types.castObject(
                                thrown/*rsh*/, fp.type/*lhsType*/, Types.ASSIGNMENT );
                    } catch( UtilEvalError e ) {
                        /*
                                  Catch the mismatch and continue to try the next
                                  Note: this is innefficient, should have an
                                  isAssignableFrom() that doesn't throw
                                  // TODO: we do now have a way to test assignment
                                  // 	in castObject(), use it?
                              */
                        continue;
                    }

                // Found match, execute catch block
                BSHBlock cb = catchBlocks.get(i);

                // Prepare to execute the block.
                // We must create a new BlockNameSpace to hold the catch
                // parameter and swap it on the stack after initializing it.

                NameSpace enclosingNameSpace = callstack.top();
                BlockNameSpace cbNameSpace =
                        new BlockNameSpace( enclosingNameSpace );

                try {
                    if ( fp.type == BSHFormalParameter.UNTYPED )
                        // set an untyped variable directly in the block
                        cbNameSpace.setBlockVariable( fp.name, thrown );
                    else
                    {
                        // set a typed variable (directly in the block)
                        Modifiers modifiers = new Modifiers();
                        cbNameSpace.setTypedVariable(
                                fp.name, fp.type, thrown, new Modifiers()/*none*/ );
                    }
                } catch ( UtilEvalError e ) {
                    throw new InterpreterError(
                            "Unable to set var in catch block namespace." );
                }

                // put cbNameSpace on the top of the stack
                callstack.swap( cbNameSpace );
                try {
                    ret = cb.accept(this);
                } finally {
                    // put it back
                    callstack.swap( enclosingNameSpace );
                }

                target = null;  // handled target
                break;
            }
        }

        // evaluate finally block
        if( finallyBlock != null ) {
            Object result = finallyBlock.accept(this);
            if( result instanceof ReturnControl )
                return result;
        }

        // exception fell through, throw it upward...
        if(target != null)
            throw target;

        if(ret instanceof ReturnControl)
            return ret;
        else
            return Primitive.VOID;
    }


    @Override
    public Object visit(BSHType node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHType class.", node);
    }


    /**
     Returns a class descriptor for this type.
     If the type is an ambiguous name (object type) evaluation is
     attempted through the namespace in order to resolve imports.
     If it is not found and the name is non-compound we assume the default
     package for the name.
     */
    public String getTypeDescriptor( BSHType node, String defaultPackage )
    {
        // return cached type if available
        if ( node.descriptor != null )
            return node.descriptor;

        String descriptor;
        //  first typeNode will either be PrimitiveType or AmbiguousName
        SimpleNode typeNode = node.getTypeNode();
        if ( typeNode instanceof BSHPrimitiveType)
            descriptor = BshInterpreterUtil.getTypeDescriptor( ((BSHPrimitiveType)typeNode).type );
        else
        {
            String clasName = ((BSHAmbiguousName)typeNode).text;
            BshClassManager bcm = interpreter.getClassManager();
            // Note: incorrect here - we are using the hack in bsh class
            // manager that allows lookup by base name.  We need to eliminate
            // this limitation by working through imports.  See notes in class
            // manager.
            String definingClass = bcm.getClassBeingDefined( clasName );

            Class clas = null;
            if ( definingClass == null )
            {
                try {
                    clas = ambiguousNameToClass(((BSHAmbiguousName)typeNode) );
                } catch ( EvalError e ) {
                    //throw new InterpreterError("unable to resolve type: "+e);
                    // ignore and try default package
                    //System.out.println("BSHType: "+typeNode+" class not found");
                }
            } else
                clasName = definingClass;

            if ( clas != null )
            {
                //System.out.println("found clas: "+clas);
                descriptor = BshInterpreterUtil.getTypeDescriptor( clas );
            }else
            {
                if ( defaultPackage == null || Name.isCompound( clasName ) )
                    descriptor = "L" + clasName.replace('.','/') + ";";
                else
                    descriptor =
                            "L"+defaultPackage.replace('.','/')+"/"+clasName + ";";
            }
        }

        for(int i=0; i<node.arrayDims; i++)
            descriptor = "["+descriptor;

        node.descriptor = descriptor;
        //System.out.println("BSHType: returning descriptor: "+descriptor);
        return descriptor;
    }


    public Class getType(BSHType tnode)
            throws EvalError
    {
        // return cached type if available
        if ( tnode.type != null )
            return tnode.type;

        //  first node will either be PrimitiveType or AmbiguousName
        SimpleNode node = tnode.getTypeNode();
        if ( node instanceof BSHPrimitiveType )
            tnode.baseType = ((BSHPrimitiveType)node).getType();
        else
            tnode.baseType = ambiguousNameToClass(((BSHAmbiguousName)node));

        if ( tnode.arrayDims > 0 ) {
            try {
                // Get the type by constructing a prototype array with
                // arbitrary (zero) length in each dimension.
                int[] dims = new int[tnode.arrayDims]; // int array default zeros
                Object obj = Array.newInstance(tnode.baseType, dims);
                tnode.type = obj.getClass();
            } catch(Exception e) {
                throw new EvalError("Couldn't construct array type",
                        tnode, callstack );
            }
        } else
            tnode.type = tnode.baseType;

        // hack... sticking to first interpreter that resolves this
        // see comments on type instance variable
        interpreter.getClassManager().addListener(tnode);

        return tnode.type;
    }


    @Override
    public Object visit(BSHTypedVariableDeclaration node) {
        try {
            NameSpace namespace = callstack.top();
            BSHType typeNode = node.getTypeNode();
            Class type = getType(typeNode);

            BSHVariableDeclarator [] bvda = node.getDeclarators();
            for (int i = 0; i < bvda.length; i++)
            {
                BSHVariableDeclarator dec = bvda[i];

                // Type node is passed down the chain for array initializers
                // which need it under some circumstances
                Object value = evalVariableDeclarator(dec, typeNode);

                try {
                    namespace.setTypedVariable(
                            dec.name, type, value, node.modifiers );
                } catch ( UtilEvalError e ) {
                    throw e.toEvalError( node, callstack );
                }
            }
        } catch ( EvalError e ) {
            e.reThrow( "Typed variable declaration" );
        }

        return Primitive.VOID;
    }

    public Class evalType( BSHTypedVariableDeclaration node )
            throws EvalError
    {
        BSHType typeNode = node.getTypeNode();
        return getType( typeNode );
    }


    @Override
    public Object visit(BSHUnaryExpression node) {
        SimpleNode simpleNode = (SimpleNode)node.jjtGetChild(0);

        // If this is a unary increment of decrement (either pre or postfix)
        // then we need an LHS to which to assign the result.  Otherwise
        // just do the unary operation for the value.
        try {
            if ( node.kind == ParserConstants.INCR || node.kind == ParserConstants.DECR ) {
                LHS lhs = primaryExprToLHS((BSHPrimaryExpression) simpleNode);
                return node.lhsUnaryOperation(lhs, interpreter.getStrictJava());
            } else
                return
                        node.unaryOperation(simpleNode.accept(this), node.kind);
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }

    /**
     Evaluate the optional initializer value.
     (The name was set at parse time.)

     A variable declarator can be evaluated with or without preceding
     type information. Currently the type info is only used by array
     initializers in the case where there is no explicitly declared type.

     @param typeNode is the BSHType node.  Its info is passed through to any
     variable intializer children for the case where the array initializer
     does not declare the type explicitly. e.g.
     int [] a = { 1, 2 };
     typeNode may be null to indicate no type information available.
     */
    private Object evalVariableDeclarator(BSHVariableDeclarator node, BSHType typeNode)
            throws EvalError
    {
        // null value means no value
        Object value = null;

        if ( node.jjtGetNumChildren() > 0 )
        {
            SimpleNode initializer = (SimpleNode)node.jjtGetChild(0);

            /*
                   If we have type info and the child is an array initializer
                   pass it along...  Else use the default eval style.
                   (This allows array initializer to handle the problem...
                   allowing for future enhancements in loosening types there).
               */
            if ( (typeNode != null)
                    && initializer instanceof BSHArrayInitializer
                    )
                value = evalArrayInitializer(((BSHArrayInitializer) initializer),
                        typeNode.getBaseType(), typeNode.getArrayDims(),
                        callstack, interpreter);
            else
                value = initializer.accept(this);
        }

        if ( value == Primitive.VOID )
            throw new EvalError("Void initializer.", node, callstack );

        return value;
    }


    @Override
    public Object visit(BSHVariableDeclarator node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHVariableDeclarator class.");
    }


    @Override
    public Object visit(BSHWhileStatement node) {
        int numChild = node.jjtGetNumChildren();

        // Order of body and condition is swapped for do / while
        final SimpleNode condExp;
        final SimpleNode body;

        if ( node.isDoStatement ) {
            condExp = (SimpleNode) node.jjtGetChild(1);
            body = (SimpleNode) node.jjtGetChild(0);
        } else {
            condExp = (SimpleNode) node.jjtGetChild(0);
            if ( numChild > 1 )	{
                body = (SimpleNode) node.jjtGetChild(1);
            } else {
                body = null;
            }
        }

        boolean doOnceFlag = node.isDoStatement;

        while (doOnceFlag || BshInterpreterUtil.evaluateCondition(condExp, this)) {
            doOnceFlag = false;
            // no body?
            if ( body == null ) {
                continue;
            }
            Object ret = body.accept(this);
            if (ret instanceof ReturnControl) {
                switch(( (ReturnControl)ret).kind ) {
                    case ParserConstants.RETURN:
                        return ret;

                    case ParserConstants.CONTINUE:
                        break;

                    case ParserConstants.BREAK:
                        return Primitive.VOID;
                }
            }
        }
        return Primitive.VOID;
    }

}
