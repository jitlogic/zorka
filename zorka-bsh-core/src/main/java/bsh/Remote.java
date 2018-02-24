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
import java.net.*;
import java.text.*;
/**
	Remote executor class. Posts a script from the command line to a BshServlet
 	or embedded  interpreter using (respectively) HTTP or the bsh telnet
	service. Output is printed to stdout and a numeric return value is scraped
	from the result.
*/
public class Remote
{
    public static void main( String args[] )
		throws Exception
	{
		if ( args.length < 2 ) {
			System.out.println(
				"usage: Remote URL(http|bsh) file [ file ] ... ");
			System.exit(1);
		}
		String url = args[0];
		String text = getFile(args[1]);
		int ret = eval( url, text );
		System.exit( ret );
		}

	/**
		Evaluate text in the interpreter at url, returning a possible integer
	 	return value.
	*/
	public static int eval( String url, String text )
		throws IOException
	{
		String returnValue = null;
		if ( url.startsWith( "http:" ) ) {
			returnValue = doHttp( url, text );
		} else if ( url.startsWith( "bsh:" ) ) {
			returnValue = doBsh( url, text );
		} else
			throw new IOException( "Unrecognized URL type."
				+"Scheme must be http:// or bsh://");

		try {
			return Integer.parseInt( returnValue );
		} catch ( Exception e ) {
			// this convention may change...
			return 0;
		}
	}

	static String doBsh( String url, String text ) 
	{ 
	    OutputStream out;
	    InputStream in;
	    String host = "";
	    String port = "";
	    String returnValue = "-1";
		String orgURL = url;
	    
		// Need some format checking here
	    try {
			url = url.substring(6); // remove the bsh://
			// get the index of the : between the host and the port is located
			int index = url.indexOf(":");
			host = url.substring(0,index);
			port = url.substring(index+1,url.length());
		} catch ( Exception ex ) {
			System.err.println("Bad URL: "+orgURL+": "+ex  );
			return returnValue;
	    }

	    try {
			System.out.println("Connecting to host : " 
				+ host + " at port : " + port);
			Socket s = new Socket(host, Integer.parseInt(port) + 1);
			
			out = s.getOutputStream();
			in = s.getInputStream();
			
			sendLine( text, out );

			BufferedReader bin = new BufferedReader( 
				new InputStreamReader(in));
			  String line;
			  while ( (line=bin.readLine()) != null )
				System.out.println( line );

			// Need to scrape a value from the last line?
			returnValue="1";
			return returnValue;
	    } catch(Exception ex) {
			System.err.println("Error communicating with server: "+ex);
			return returnValue;
	    }
	}

    private static void sendLine( String line, OutputStream outPipe )
		throws IOException
	{
		outPipe.write( line.getBytes() );
		outPipe.flush();
    }


	/*
		TODO: this is not unicode friendly, nor is getFile()
		The output is urlencoded 8859_1 text.
		should probably be urlencoded UTF-8... how does the servlet determine
		the encoded charset?  I guess we're supposed to add a ";charset" clause
		to the content type?
	*/
	static String doHttp( String postURL, String text )
	{
		String returnValue = null;
		StringBuilder sb = new StringBuilder();
		sb.append( "bsh.client=Remote" );
		sb.append( "&bsh.script=" );
		sb.append( URLEncoder.encode( text ) );
		/*
		// This requires Java 1.3
		try {
			sb.append( URLEncoder.encode( text, "8859_1" ) );
		} catch ( UnsupportedEncodingException e ) {
			e.printStackTrace();
		}
		*/
		String formData = sb.toString(  );

		try {
		  URL url = new URL( postURL );
		  HttpURLConnection urlcon =
			  (HttpURLConnection) url.openConnection(  );
		  urlcon.setRequestMethod("POST");
		  urlcon.setRequestProperty("Content-type",
			  "application/x-www-form-urlencoded");
		  urlcon.setDoOutput(true);
		  urlcon.setDoInput(true);
		  PrintWriter pout = new PrintWriter( new OutputStreamWriter(
			  urlcon.getOutputStream(), "8859_1"), true );
		  pout.print( formData );
		  pout.flush();

		  // read results...
		  int rc = urlcon.getResponseCode();
		  if ( rc != HttpURLConnection.HTTP_OK )
			System.out.println("Error, HTTP response: "+rc );

		  returnValue = urlcon.getHeaderField("Bsh-Return");

		  BufferedReader bin = new BufferedReader(
			new InputStreamReader( urlcon.getInputStream() ) );
		  String line;
		  while ( (line=bin.readLine()) != null )
			System.out.println( line );

		  System.out.println( "Return Value: "+returnValue );

		} catch (MalformedURLException e) {
		  System.out.println(e);     // bad postURL
		} catch (IOException e2) {
		  System.out.println(e2);    // I/O error
		}

		return returnValue;
	}

	/*
		Note: assumes default character encoding
	*/
	static String getFile( String name )
		throws FileNotFoundException, IOException
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader bin = new BufferedReader( new FileReader( name ) );
		String line;
		while ( (line=bin.readLine()) != null )
			sb.append( line ).append( "\n" );
		return sb.toString();
	}

}
