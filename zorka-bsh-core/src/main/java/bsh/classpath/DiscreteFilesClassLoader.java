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

package bsh.classpath;

import java.io.*;
import java.io.File;
import java.util.*;
import java.awt.*;
import bsh.BshClassManager;
import bsh.classpath.BshClassPath.ClassSource;
import bsh.classpath.BshClassPath.DirClassSource;
import bsh.classpath.BshClassPath.GeneratedClassSource;

/**
	A classloader which can load one or more classes from specified sources.
	Because the classes are loaded via a single classloader they change as a
	group and any versioning cross dependencies can be managed.
*/
public class DiscreteFilesClassLoader extends BshClassLoader 
{
	/**
		Map of class sources which also implies our coverage space.
	*/
	ClassSourceMap map;

	public static class ClassSourceMap extends HashMap 
	{
		public void put( String name, ClassSource source ) {
			super.put( name, source );
		}
		public ClassSource get( String name ) {
			return (ClassSource)super.get( name );
		}
	}
	
	public DiscreteFilesClassLoader( 
		BshClassManager classManager, ClassSourceMap map ) 
	{
		super( classManager );
		this.map = map;
	}

	/**
	*/
	public Class findClass( String name ) throws ClassNotFoundException 
	{
		// Load it if it's one of our classes
		ClassSource source = map.get( name );

		if ( source != null )
		{
			byte [] code = source.getCode( name );
			return defineClass( name, code, 0, code.length );
		} else
			// Let superclass BshClassLoader (URLClassLoader) findClass try 
			// to find the class...
			return super.findClass( name );
	}

	public String toString() {
		return super.toString() + "for files: "+map;
	}

}
