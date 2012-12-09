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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Lexer implements Iterable<Token>, Iterator<Token> {

    protected final static int E = -1;


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
    private int pos = 0;
    private String input;


    protected Lexer(String input, byte[][] lextab) {
        this.input = input;
        this.lextab = lextab;
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

        int cur = pos;
        int type = lextab[0][getch(cur++)], state = type;

        while (state >= 0 && cur < input.length()) {
            state = lextab[state][getch(cur)];
            type = state >= 0 ? state : type;
            cur = state == -1 ? cur : cur + 1;
        }

        String s = input.substring(pos, cur); pos = cur;

        return new Token(type >= 0 ? type : 0, s);
    }


    public void remove() {
        throw new UnsupportedOperationException();
    }

}
