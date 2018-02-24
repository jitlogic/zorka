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


package bsh.ast;

import bsh.*;

public final class BSHLiteral extends SimpleNode
{
	public static volatile boolean internStrings = true;

	public Object value;

	public BSHLiteral(int id) { super(id); }

	private char getEscapeChar(char ch)
	{
		switch(ch)
		{
			case 'b':
				ch = '\b';
				break;

			case 't':
				ch = '\t';
				break;

			case 'n':
				ch = '\n';
				break;

			case 'f':
				ch = '\f';
				break;

			case 'r':
				ch = '\r';
				break;

			// do nothing - ch already contains correct character
			case '"':
			case '\'':
			case '\\':
				break;
		}

		return ch;
	}

	public void charSetup(String str)
	{
		char ch = str.charAt(0);
		if(ch == '\\')
		{
			// get next character
			ch = str.charAt(1);

			if(Character.isDigit(ch))
				ch = (char)Integer.parseInt(str.substring(1), 8);
			else
				ch = getEscapeChar(ch);
		}

		value = new Primitive(new Character(ch).charValue());
	}

	public void stringSetup(String str)
	{
		StringBuilder buffer = new StringBuilder();
		int len = str.length();
		for(int i = 0; i < len; i++)
		{
			char ch = str.charAt(i);
			if(ch == '\\')
			{
				// get next character
				ch = str.charAt(++i);

				if(Character.isDigit(ch))
				{
					int endPos = i;

					// check the next two characters
					int max = Math.min( i + 2, len - 1 );
					while(endPos < max)
					{
						if(Character.isDigit(str.charAt(endPos + 1)))
							endPos++;
						else
							break;
					}

					ch = (char)Integer.parseInt(str.substring(i, endPos + 1), 8);
					i = endPos;
				}
				else
					ch = getEscapeChar(ch);
			}

			buffer.append(ch);
		}

		String s = buffer.toString();
		if( internStrings )
			s = s.intern();
		value = s;
	}

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
