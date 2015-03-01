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

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Map;

/**
 * Lexical analyzer for LDAP queries.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LdapLexer extends Lexer {

    /** Unknown character (possibly illegal) */
    private static final byte CH_UNKNOWN     = 0;

    /** Whitespace character */
    private static final byte CH_WHITESPACE  = 1;

    /** Alphanumeric character (parts of symbols) */
    private static final byte CH_ALNUM       = 2;

    /** Left par (delimiters in LDAP queries) */
    private static final byte CH_LPAREN      = 3;

    /** Right par (delimiters in LDAP queries) */
    private static final byte CH_RPAREN      = 4;

    /** Exclamation mark (logical operator) */
    private static final byte CH_EXCLAMATION = 5;

    /** Ampersand character (logical operator) */
    private static final byte CH_AMPERSAND   = 6;

    /** Vertical bar (logical operator) */
    private static final byte CH_VERTBAR     = 7;

    /* Equals character (comparison operator) */
    private static final byte CH_EQUALS      = 8;

    /** Angle characters (comparison operator) */
    private static final byte CH_ANGLE       = 9;

    //private static final int S_WHITESPACE = 1; // White space
    //private static final int S_FSTART     = 2; // filter starting delimiter
    //private static final int S_FSTOP      = 3; // filter ending delimiter
    //private static final int S_OPERATOR   = 4; // filter operator: '!', '~',
    //private static final int S_MATCHATTR  = 5; // matching attribute (plus rule)
    //private static final int S_MATCHOPER1 = 6; // matching operator (short)
    //private static final int S_MATCHOPER2 = 7; // matching operator (long)
    //private static final int S_MATCHVALUE = 8; // matching value
    //private static final int S_ERROR      = 9; // illegal character

    /** Character map for LDAP queries */
    private static final Map<Character, Byte> CHMAP = ZorkaUtil.map(
            '(', CH_LPAREN, ')', CH_RPAREN, '~', CH_ANGLE,
            '!', CH_EXCLAMATION, '&', CH_AMPERSAND, '|', CH_VERTBAR,
            '=', CH_EQUALS, '<', CH_ANGLE, '>', CH_ANGLE
    );

    /** Initializes character tab for LDAP queries */
    private static byte[] initChTab() {
        byte[] tab = new byte[128];

        for (int i = 0; i < 128; i++) {
            if (CHMAP.containsKey((char) i)) {
                tab[i] = CHMAP.get((char)i);
            } else if (Character.isWhitespace(i)) {
                tab[i] = CH_WHITESPACE;
            } else {
                tab[i] = CH_ALNUM;
            }
        }

        return tab;
    }

    /** Character map for LDAP queries */
    private static final byte[] CHT_LDAP = initChTab();

    /** Transition table for LDAP lexer */
    private static final byte[][] LEX_LDAP = {
                  //       UN WS AN  (  )  !  &  |  = <~>
            lxtab(CHT_LDAP, E, 1, E, 2, 2, E, E, E, E, E), // 0 = S_START
            lxtab(CHT_LDAP, E, 1, E,-2,-9, E, E, E, E, E), // 1 = S_WHITESPACE
            lxtab(CHT_LDAP, E,-5,-5,-2,-3,-4,-4,-4,-9,-9), // 2 = S_FSTART
            lxtab(CHT_LDAP, E,-1,-9,-2,-3,-9,-9,-9,-9,-9), // 3 = S_FSTOP
            lxtab(CHT_LDAP, E,-1,-7,-2,-2,-4,-4,-4,-9,-9), // 4 = S_OPERATOR
            lxtab(CHT_LDAP, E, 5, 5, 5, 5, 5, 5, 5,-6,-7), // 5 = S_MATCHATTR
            lxtab(CHT_LDAP, E,-8,-8,-8,-4,-8,-8,-8,-8,-8), // 6 = S_MATCHOPER1
            lxtab(CHT_LDAP, E, 9, 9, 9, 9, 9, 9, 9, 6, 9), // 7 = S_MATCHOPER2
            lxtab(CHT_LDAP, E, 8, 8, 8,-3, 8, 8, 8, 8, 8), // 8 = S_MATCHVALUE
            lxtab(CHT_LDAP, E, E, E, E, E, E, E, E, E, E), // 9 = S_ERROR
    };

    /** State-to-token-type map */
    private static final int[] tokenTypes = {
            T_UNKNOWN,      // 0 = S_START
            T_WHITESPACE,   // 1 = S_WHITESPACE
            T_OPERATOR,     // 2 = S_FSTART
            T_OPERATOR,     // 3 = S_FSTOP
            T_OPERATOR,     // 4 = S_OPERATOR
            T_SYMBOL,       // 5 = S_MATCHATTR
            T_OPERATOR,     // 6 = S_MATCHOPER1
            T_OPERATOR,      // 7 = S_MATCHOPRE2
            T_LITERAL,      // 8 = S_MATCHVALUE
            T_UNKNOWN,      // 9 = S_ERROR
    };

    /**
     * Standard constructor
     *
     * @param input input string
     */
    public LdapLexer(String input) {
        super(input, LEX_LDAP, tokenTypes);
    }


    @Override
    public Lexer lex(String input) {
        return new LdapLexer(input);
    }
}
