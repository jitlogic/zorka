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

import static com.jitlogic.zorka.normproc.XqlLexer.*;

public final class GenericNormalizer implements Normalizer {

    private static final String PHD = "?";
    private static final boolean T = true, F = false;


    private static final boolean[][] XQLJOINTS = {
           // U  W  S  O  L  C  K  P
            { F, F, F, F, F, F, F, F }, // U (UNKNOWN)
            { F, F, F, F, F, F, F, F }, // W (WHITESPACE)
            { F, F, T, F, T, F, T, F }, // S (SYMBOL)
            { F, F, F, F, F, F, T, F }, // O (OPERATOR)
            { F, F, T, F, T, F, T, F }, // L (LITERAL)
            { F, F, F, F, F, F, F, F }, // C (COMMENT)
            { F, F, T, T, T, F, T, F }, // K (KEYWORD)
            { F, F, F, F, F, F, F, F }, // P (PLACEHOLDER)
    };

    private static final boolean[][] XQLPROC = {
           // U  W  S  O  L  C  K  P
            { T, T, F, F, F, T, F, F }, // Tokens to be cut off;
            { F, F, F, F, T, F, F, T }, // Tokens to be replaced by placeholders;
            { F, F, T, F, F, F, T, F }, // Tokens to be converted to upper (lower) case;
            { F, F, F, F, F, F, F, F }, // Tokens to be trimmed;
    };

    private static final boolean[][] LDAPJOINTS = {
           // U  W  S  O  L  C  K  P
            { F, F, F, F, F, F, F, F }, // U (UNKNOWN)
            { F, F, F, F, F, F, F, F }, // W (WHITESPACE)
            { F, F, F, F, F, F, F, F }, // S (SYMBOL)
            { F, F, F, F, F, F, F, F }, // O (OPERATOR)
            { F, F, F, F, F, F, F, F }, // L (LITERAL)
            { F, F, F, F, F, F, F, F }, // C (COMMENT)
            { F, F, F, F, F, F, F, F }, // K (KEYWORD)
            { F, F, F, F, F, F, F, F }, // P (PLACEHOLDER)
    };

    private static final boolean[][] LDAPPROC = {
           // U  W  S  O  L  C  K  P
            { T, T, F, F, F, T, F, F }, // Tokens to be cut off;
            { F, F, F, F, T, F, F, T }, // Tokens to be replaced by placeholders;
            { F, F, T, F, F, F, T, F }, // Tokens to be converted to upper (lower) case;
            { F, F, T, F, T, F, T, F }, // Tokens to be trimmed
    };


    private int flags;
    private Lexer template;
    private boolean[][] joints, proc;
    private boolean upcase = false;


    public static Normalizer xql(int dialect, int flags) {
        return new GenericNormalizer(new XqlLexer(dialect, ""), flags, XQLJOINTS, XQLPROC);
    }


    public static Normalizer ldap(int flags) {
        return new GenericNormalizer(new LdapLexer(""), flags, LDAPJOINTS, LDAPPROC);
    }


    private GenericNormalizer(Lexer template, int flags, boolean[][] joints, boolean[][] proc) {
        this.template = template;
        this.flags = flags;
        this.joints = joints;
        this.proc = proc;
    }


    public String normalize(String input) {

        if (input == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer(input.length()+2);
        Lexer lexer = template.lex(input);
        int last = T_UNKNOWN;

        while (lexer.hasNext()) {
            Token token = lexer.next();
            int t = token.getType();
            String s = token.getContent();

            if (0 != (flags & (1<<t))) {
                if (proc[0][t]) { continue; }
                if (proc[1][t]) { s = PHD; }
                if (proc[2][t]) { s = upcase ? s.toUpperCase() : s.toLowerCase(); }
                if (proc[3][t]) { s = s.trim(); }
            }

            if (joints[last][t]) {
                sb.append(" ");
            }

            sb.append(s);
            last = t;
        }

        return sb.toString();
    }

}
