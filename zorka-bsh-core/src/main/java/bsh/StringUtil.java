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

import java.util.*;

public class StringUtil {

	public static String [] split( String s, String delim) {
		List<String> v = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(s, delim);
		while ( st.hasMoreTokens() )
			v.add( st.nextToken() );
		return v.toArray(new String[0]);
	}

	public static String maxCommonPrefix( String one, String two ) {
		int i=0;
		while( one.regionMatches( 0, two, 0, i ) )
			i++;
		return one.substring(0, i-1);
	}

    public static String methodString(String name, Class[] types)
    {
        StringBuilder sb = new StringBuilder(name + "(");
        if ( types.length > 0 )
			sb.append(" ");
        for( int i=0; i<types.length; i++ )
        {
            Class c = types[i];
            sb.append( ( (c == null) ? "null" : c.getName() ) 
				+ ( i < (types.length-1) ? ", " : " " ) );
        }
        sb.append(")");
        return sb.toString();
    }

	/**
		Split a filename into dirName, baseName
		@return String [] { dirName, baseName }
    public String [] splitFileName( String fileName ) 
	{ 
		String dirName, baseName;
		int i = fileName.lastIndexOf( File.separator );
		if ( i != -1 ) {
			dirName = fileName.substring(0, i);
			baseName = fileName.substring(i+1);
		} else
			baseName = fileName;

		return new String[] { dirName, baseName };
	}

	*/

	/**
		Hack - The real method is in Reflect.java which is not public.
	*/
    public static String normalizeClassName( Class type )
	{
		return Reflect.normalizeClassName( type );
	}
}
