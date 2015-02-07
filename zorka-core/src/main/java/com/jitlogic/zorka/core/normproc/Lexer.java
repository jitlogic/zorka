/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.normproc;

import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Implements simplified lexing algorithm useful for implementing normalizers.
 * Note that this lexer has not backtracking, so concrete lexers (eg. for LDAP or SQL)
 * are not always precise and can sometimes generate incorrect results. It isn't big issue
 * for simple normalization purposes and speed/simplicity is priority here, so it is left
 * this way and eventual fixes will be done only when needed.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public abstract class Lexer implements Iterable<Token>, Iterator<Token> {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(Lexer.class);

    /** Unknown token */
    public static final int T_UNKNOWN     = 0;

    /** Whitespace token */
    public static final int T_WHITESPACE  = 1;

    /** Symbol token */
    public static final int T_SYMBOL      = 2;

    /** Operator token */
    public static final int T_OPERATOR    = 3;

    /** Literal token */
    public static final int T_LITERAL     = 4;

    /** Comment token */
    public static final int T_COMMENT     = 5;

    /** Keyword token */
    public static final int T_KEYWORD     = 6;

    /** Placeholder token */
    public static final int T_PLACEHOLDER = 7;

    /** Start state */
    protected final static int S_START = 0;      // Starting state

    /** End-of-token marker for lexer tabs */
    protected final static int E = -127;

    /**
     * Reads keyword files from classpath. Returns map of sets of strings (eg. map
     * of sets of keyword files for various SQL dialects).
     *
     * @param path path to keyword file (it has to be classpath)
     *
     * @return map of keyword sets
     */
    protected static Map<String,Set<String>> readKeywordFile(String path) {

        Properties props = new Properties();
        InputStream is = Lexer.class.getResourceAsStream(path);

        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error(ZorkaLogger.ZAG_ERRORS, "Error closing property file '"+path+"':", e);
                }
            }
        }

        Map<String,Set<String>> kwmap = new HashMap<String, Set<String>>();

        for (Object name : props.keySet()) {
            kwmap.put(name.toString(), strSet(props.getProperty(name.toString()).split("\\,")));
        }

        return Collections.unmodifiableMap(kwmap);
    }


    /**
     * Creates a set from supplied strings.
     *
     * @param strings members of newly formed set
     *
     * @return set of strings
     */
    protected static Set<String> strSet(String...strings) {
        Set<String> set = new HashSet<String>(strings.length * 2 + 1);
        for (String s : strings) {
            set.add(s.trim().toLowerCase());
        }

        return Collections.unmodifiableSet(set);
    }


    /**
     * Creates table of lexer tab from character map and transitions
     *
     * @param chtab character tab
     *
     * @param transitions transitions
     *
     * @return lexer tab
     */
    protected static byte[] lxtab(byte[] chtab, int...transitions) {
        byte[] lxtab = new byte[chtab.length];

        for (int i = 0; i < lxtab.length; i++) {
            lxtab[i] = (byte)transitions[chtab[i]];
        }

        return lxtab;
    }


    /** Lexer tab */
    private byte[][] lextab;

    /** Current position */
    private int pos;

    /** Current state */
    private int state = S_START;

    /** Input string */
    private String input;

    /** State-to-token-types map */
    private int[] tokenTypes;


    /**
     * Standard constructor
     *
     * @param input input string
     *
     * @param lextab lexer tab
     *
     * @param tokenTypes state-to-token-type map
     */
    protected Lexer(String input, byte[][] lextab, int[] tokenTypes) {
        this.input = input;
        this.lextab = lextab;
        this.tokenTypes = tokenTypes;
    }


    /**
     * Returns one character from input string. Handles special (unicode) characters.
     *
     * @param pos position in input string
     *
     * @return integer representation of a character
     */
    protected int getch(int pos) {

        char ch = input.charAt(pos);

        if (ch >= 128) {
            return Character.isJavaIdentifierStart(ch) ? 65 : 0;
        }

        return (int)ch;
    }


    @Override
    public Iterator<Token> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return pos < input.length();
    }


    @Override
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


    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates clone of lexer object with new input string (but clones configuration settings).
     * This is used in normalizer that creates lexer template at construction time and then
     * uses it as a template when processing data.
     *
     * @param input input string
     *
     * @return new lexer with new input but original configuration
     */
    public abstract Lexer lex(String input);
}
