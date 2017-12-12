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

public class Variable implements java.io.Serializable 
{
	static final int DECLARATION=0, ASSIGNMENT=1;
	/** A null type means an untyped variable */
	String name;
	Class type = null;
	String typeDescriptor;
	Object value;
	Modifiers modifiers;
	LHS lhs;

	Variable( String name, Class type, LHS lhs ) 
	{
		this.name = name;
		this.lhs = lhs;
		this.type = type;
	}
	
	Variable( String name, Object value, Modifiers modifiers )
		throws UtilEvalError
	{
		this( name, (Class)null/*type*/, value, modifiers );
	}

	/**
		This constructor is used in class generation.
	*/
	Variable( 
		String name, String typeDescriptor, Object value, Modifiers modifiers 
	)
		throws UtilEvalError
	{
		this( name, (Class)null/*type*/, value, modifiers );
		this.typeDescriptor = typeDescriptor;
	}

	/**
		@param value may be null if this 
	*/
	Variable( String name, Class type, Object value, Modifiers modifiers )
		throws UtilEvalError
	{

		this.name=name;
		this.type =	type;
		this.modifiers = modifiers;
		setValue( value, DECLARATION );
	}

	/**
		Set the value of the typed variable.
		@param value should be an object or wrapped bsh Primitive type.
		if value is null the appropriate default value will be set for the
		type: e.g. false for boolean, zero for integer types.
	*/
	public void setValue( Object value, int context ) 
		throws UtilEvalError
	{

		// check this.value
		if ( hasModifier("final") && this.value != null )
			throw new UtilEvalError ("Final variable, can't re-assign.");

		if ( value == null )
			value = Primitive.getDefaultValue( type );

		if ( lhs != null )
		{
			lhs.assign( value, false/*strictjava*/ );
			return;
		}

		// TODO: should add isJavaCastable() test for strictJava
		// (as opposed to isJavaAssignable())
		if ( type != null )
			value = Types.castObject( value, type, 
				context == DECLARATION ? Types.CAST : Types.ASSIGNMENT
			);

		this.value= value;
	}

	/*
		Note: UtilEvalError here comes from lhs.getValue().
		A Variable can represent an LHS for the case of an imported class or
		object field.
	*/
	Object getValue() 
		throws UtilEvalError
	{ 
		if ( lhs != null )
			return lhs.getValue();

		return value; 
	}

	/** A type of null means loosely typed variable */
	public Class getType() { return type;	}

	public String getTypeDescriptor() { return typeDescriptor; }

	public Modifiers getModifiers() { return modifiers; }

	public String getName() { return name; }

	public boolean hasModifier( String name ) {
		return modifiers != null && modifiers.hasModifier(name);
	}

	public String toString() { 
		return "Variable: "+super.toString()+" "+name+", type:"+type
			+", value:"+value +", lhs = "+lhs;
	}
}
