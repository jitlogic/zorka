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
	This is a quick hack to turn empty lines entered interactively on the 
	command line into ';\n' empty lines for the interpreter.  It's just more 
	pleasant to be able to hit return on an empty line and see the prompt 
	reappear.
		
	This is *not* used when text is sourced from a file non-interactively.
*/
class CommandLineReader extends FilterReader {

    public CommandLineReader( Reader in ) {
		super(in);
    }

	static final int 
		normal = 0,
		lastCharNL = 1,
		sentSemi = 2;

	int state = lastCharNL;

    public int read() throws IOException {
		int b;

		if ( state == sentSemi ) {
			state = lastCharNL;
			return '\n';
		}

		// skip CR
        while ( (b = in.read()) == '\r' );

		if ( b == '\n' )
			if ( state == lastCharNL ) {
				b = ';';
				state = sentSemi;
			} else
				state = lastCharNL;
		else
			state = normal;

		return b;
    }

	/**
		This is a degenerate implementation.
		I don't know how to keep this from blocking if we try to read more
		than one char...  There is no available() for Readers ??
	*/
    public int read(char buff[], int off, int len) throws IOException 
	{
		int b = read();
		if ( b == -1 )
			return -1;  // EOF, not zero read apparently
		else {
			buff[off]=(char)b;
			return 1;
		}
    }

	// Test it
	public static void main( String [] args ) throws Exception {
		Reader in = new CommandLineReader( new InputStreamReader(System.in) );
		while ( true )
			System.out.println( in.read() );
		
	}
}

