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

import java.io.*;

/**
	The capabilities of a minimal console for BeanShell.
	Stream I/O and optimized print for output.

	A simple console may ignore some of these or map them to trivial
	implementations.  e.g. print() with color can be mapped to plain text.
	@see bsh.util.GUIConsoleInterface
*/
public interface ConsoleInterface {
	public Reader getIn();
	public PrintStream getOut();
	public PrintStream getErr();
	public void println( Object o );
	public void print( Object o );
	public void error( Object o );
}

