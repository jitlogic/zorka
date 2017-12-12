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

import java.util.Hashtable;

/**
	The map of extended features supported by the runtime in which we live.
	<p>

	This class should be independent of all other bsh classes!
	<p>

	Note that tests for class existence here do *not* use the 
	BshClassManager, as it may require other optional class files to be 
	loaded.  
*/
public class Capabilities 
{
	private static volatile boolean accessibility = false;

	public static boolean haveSwing() {
		// classExists caches info for us
		return classExists( "javax.swing.JButton" );
	}

	/**
		If accessibility is enabled
		determine if the accessibility mechanism exists and if we have
		the optional bsh package to use it.
		Note that even if both are true it does not necessarily mean that we 
		have runtime permission to access the fields... Java security has
	 	a say in it.
		@see bsh.ReflectManager
	*/
	public static boolean haveAccessibility() 
	{
		return accessibility;
	}

	public static void setAccessibility( boolean b ) 
		throws Unavailable
	{ 
		if ( b == false )
		{
			accessibility = false;
		} else {

			// test basic access
			try {
				String.class.getDeclaredMethods();
			} catch ( SecurityException e ) {
				throw new Unavailable("Accessibility unavailable: "+e);
			}
	
			accessibility = true;
		}
		BshClassManager.clearResolveCache();
	}

	private static Hashtable classes = new Hashtable();
	/**
		Use direct Class.forName() to test for the existence of a class.
		We should not use BshClassManager here because:
			a) the systems using these tests would probably not load the
			classes through it anyway.
			b) bshclassmanager is heavy and touches other class files.  
			this capabilities code must be light enough to be used by any
			system **including the remote applet**.
	*/
	public static boolean classExists( String name ) 
	{
		Object c = classes.get( name );

		if ( c == null ) {
			try {
				/*
					Note: do *not* change this to 
					BshClassManager plainClassForName() or equivalent.
					This class must not touch any other bsh classes.
				*/
				c = Class.forName( name );
			} catch ( ClassNotFoundException e ) { }

			if ( c != null )
				classes.put(c,"unused");
		}

		return c != null;
	}

	/**
		An attempt was made to use an unavailable capability supported by
		an optional package.  The normal operation is to test before attempting
		to use these packages... so this is runtime exception.
	*/
	public static class Unavailable extends UtilEvalError
	{
		public Unavailable(String s ){ super(s); }
	}
}


