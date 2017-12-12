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

package	bsh;

/**
    A specialized namespace	for Blocks (e.g. the body of a "for" statement).
	The Block acts like a child namespace but only for typed variables 
	declared within it (block local scope) or untyped variables explicitly set 
	in it via setBlockVariable().  Otherwise variable assignment 
	(including untyped variable usage) acts like it is part of the containing
	block.  
	<p>
*/
/*
	Note: This class essentially just delegates most of its methods to its
	parent.  The setVariable() indirection is very small.  We could probably
	fold this functionality back into the base NameSpace as a special case.
	But this has changed a few times so I'd like to leave this abstraction for
	now.
*/
public class BlockNameSpace extends NameSpace
{
    public BlockNameSpace( NameSpace parent ) 
		throws EvalError
	{
		super( parent, parent.getName()+ "/BlockNameSpace" );
    }

	/**
		Override the standard namespace behavior to make assignments
		happen in our parent (enclosing) namespace, unless the variable has
		already been assigned here via a typed declaration or through
		the special setBlockVariable() (used for untyped args in try/catch).
		<p>
		i.e. only allow typed var declaration to happen in this namespace.
		Typed vars are handled in the ordinary way local scope.  All untyped
		assignments are delegated to the enclosing context.
	*/
	/*
		Note: it may see like with the new 1.3 scoping this test could be
		removed, but it cannot.  When recurse is false we still need to set the
		variable in our parent, not here.
	*/
    public void	setVariable( 
		String name, Object value, boolean strictJava, boolean recurse ) 
		throws UtilEvalError 
	{
		if ( weHaveVar( name ) ) 
			// set the var here in the block namespace
			super.setVariable( name, value, strictJava, false );
		else
			// set the var in the enclosing (parent) namespace
			getParent().setVariable( name, value, strictJava, recurse );
    }

	/**
		Set an untyped variable in the block namespace.
		The BlockNameSpace would normally delegate this set to the parent.
		Typed variables are naturally set locally.
		This is used in try/catch block argument. 
	*/
    public void	setBlockVariable( String name, Object value ) 
		throws UtilEvalError 
	{
		super.setVariable( name, value, false/*strict?*/, false );
	}

	/**
		We have the variable: either it was declared here with a type, giving
		it block local scope or an untyped var was explicitly set here via
		setBlockVariable().
	*/
	private boolean weHaveVar( String name ) 
	{
		// super.variables.containsKey( name ) not any faster, I checked
		try {
			return super.getVariableImpl( name, false ) != null;
		} catch ( UtilEvalError e ) { return false; }
	}

/**
		Get the actual BlockNameSpace 'this' reference.
		<p/>
		Normally a 'this' reference to a BlockNameSpace (e.g. if () { } )
		resolves to the parent namespace (e.g. the namespace containing the
		"if" statement).  However when code inside the BlockNameSpace needs to
		resolve things relative to 'this' we must use the actual block's 'this'
		reference.  Name.java is smart enough to handle this using
		getBlockThis().
		@see #getThis( Interpreter )
    This getBlockThis( Interpreter declaringInterpreter ) 
	{
		return super.getThis( declaringInterpreter );
	}
*/

	//
	// Begin methods which simply delegate to our parent (enclosing scope) 
	//

	/**
		This method recurses to find the nearest non-BlockNameSpace parent.

	public NameSpace getParent() 
	{
		NameSpace parent = super.getParent();
		if ( parent instanceof BlockNameSpace )
			return parent.getParent();
		else
			return parent;
	}
*/
	/** do we need this? */
	private NameSpace getNonBlockParent() 
	{
		NameSpace parent = super.getParent();
		if ( parent instanceof BlockNameSpace )
			return ((BlockNameSpace)parent).getNonBlockParent();
		else
			return parent;
	}

	/**
		Get a 'this' reference is our parent's 'this' for the object closure.
		e.g. Normally a 'this' reference to a BlockNameSpace (e.g. if () { } )
		resolves to the parent namespace (e.g. the namespace containing the
		"if" statement). 
		@see #getBlockThis( Interpreter )
	*/
    public This getThis( Interpreter declaringInterpreter ) {
		return getNonBlockParent().getThis( declaringInterpreter );
	}

	/**
		super is our parent's super
	*/
    public This getSuper( Interpreter declaringInterpreter ) {
		return getNonBlockParent().getSuper( declaringInterpreter );
	}

	/**
		delegate import to our parent
	*/
    public void	importClass(String name) {
		getParent().importClass( name );
	}

	/**
		delegate import to our parent
	*/
    public void	importPackage(String name) {
		getParent().importPackage( name );
	}

    public void	setMethod(BshMethod method) 
		throws UtilEvalError
	{
		getParent().setMethod( method );
	}
}

