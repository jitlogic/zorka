/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.normproc;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Lexer implements Iterable<Token>, Iterator<Token> {

    public final static int T_UNKNOWN     = 0;
    public final static int T_WHITESPACE  = 1;
    public final static int T_SYMBOL      = 2;
    public final static int T_OPERATOR    = 3;
    public final static int T_LITERAL     = 4;
    public final static int T_COMMENT     = 5;
    public final static int T_KEYWORD     = 6;
    public final static int T_PLACEHOLDER = 7;


    protected final static int S_START = 0;      // Starting state

    protected final static int E = -127;


    protected static Set<String> strSet(String...strings) {
        Set<String> set = new HashSet<String>(strings.length * 2 + 1);
        for (String s : strings) {
            set.add(s.toLowerCase());
        }

        return Collections.unmodifiableSet(set);
    }


    protected static byte[] lxtab(byte[] chtab, int...transitions) {
        byte[] lxtab = new byte[chtab.length];

        for (int i = 0; i < lxtab.length; i++) {
            lxtab[i] = (byte)transitions[chtab[i]];
        }

        return lxtab;
    }


    private byte[][] lextab;
    private int pos = 0, state = S_START;
    private String input;
    private int[] tokenTypes;


    protected Lexer(String input, byte[][] lextab, int[] tokenTypes) {
        this.input = input;
        this.lextab = lextab;
        this.tokenTypes = tokenTypes;
    }


    protected int getch(int pos) {

        char ch = input.charAt(pos);

        if (ch >= 128) {
            return Character.isJavaIdentifierStart(ch) ? 65 : 0;
        }

        return (int)ch;
    }



    public Iterator<Token> iterator() {
        return this;
    }


    public boolean hasNext() {
        return pos < input.length();
    }


    public Token next() {

        int cur = pos, type = state == S_START ? lextab[state][getch(cur)] : state;

        state = type; cur++;

        while (state >= 0 && cur < input.length()) {
            state = lextab[state][getch(cur)];
            if (state >= 0) {
                type = state;
                cur++;
            }
        }

        if (state < 0) {
            state = (state == E) ? S_START : Math.abs(state);
        }

        String s = input.substring(pos, cur);
        type = type == E ? 0 : Math.abs(type);
        pos = cur;

        return new Token(tokenTypes[type], s);
    }


    public void remove() {
        throw new UnsupportedOperationException();
    }

}
