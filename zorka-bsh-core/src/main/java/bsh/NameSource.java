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

/**
	This interface supports name completion, which is used primarily for 
	command line tools, etc.  It provides a flat source of "names" in a 
	space.  For example all of the classes in the classpath or all of the 
	variables in a namespace (or all of those).
	<p>
	NameSource is the lightest weight mechanism for sources which wish to
	support name completion.  In the future it might be better for NameSpace
	to implement NameCompletion directly in a more native and efficient 
	fasion.  However in general name competion is used for human interaction
	and therefore does not require high performance.
	<p>
	@see bsh.util.NameCompletion
	@see bsh.util.NameCompletionTable
*/
public interface NameSource 
{
	public String [] getAllNames();
	public void addNameSourceListener( NameSource.Listener listener );

	public static interface Listener {
		public void nameSourceChanged( NameSource src );
		/**
			Provide feedback on the progress of mapping a namespace
			@param msg is an update about what's happening
			@perc is an integer in the range 0-100 indicating percentage done
		public void nameSourceMapping( 
			NameSource src, String msg, int perc );
		*/
	}
}
